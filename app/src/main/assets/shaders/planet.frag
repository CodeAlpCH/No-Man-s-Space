#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec3 v_worldPos;
varying vec3 v_localPos;

uniform vec3  u_camPos;
uniform vec3  u_baseColor;
uniform vec3  u_glowColor;
uniform float u_time;
uniform float u_pullT;
uniform float u_type;
uniform float u_seed;

float hash(vec3 p){p=fract(p*vec3(443.897,441.423,437.195));p+=dot(p,p.yzx+19.19);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p);vec3 f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){return vnoise(p)*0.62+vnoise(p*2.1+0.73)*0.38;}

void main(){
    vec3  n      = normalize(v_normal);
    vec3  vdir   = normalize(u_camPos - v_worldPos);
    float rim    = 1.0 - max(0.0, dot(n, vdir));
    float rimAtm = pow(rim, 1.3);
    float rimCore= pow(rim, 5.0);

    // Slow planet rotation
    float rSin = sin(u_time*0.05 + u_seed*6.28);
    float rCos = cos(u_time*0.05 + u_seed*6.28);
    vec3 rp = vec3(v_localPos.x*rCos - v_localPos.z*rSin,
                   v_localPos.y,
                   v_localPos.x*rSin + v_localPos.z*rCos);

    // Strong ambient – nothing goes dark
    float diff = max(0.0, dot(n, normalize(vec3(0.55,0.85,-0.25)))) * 0.30 + 0.70;

    vec3  finalColor;
    float finalAlpha;

    // ── DUST (engine glow, type 0) ────────────────────────────────────────────
    if (u_type < 0.5) {
        float g = pow(rim, 0.85)*2.2 + 0.3;
        finalColor = u_glowColor * g;
        finalAlpha = g * 0.80;

    // ── ASTEROID (type 1) ─────────────────────────────────────────────────────
    } else if (u_type < 1.5) {
        float surf = mix(fbm(rp*3.0+u_seed*5.0), fbm(rp*6.5+1.73), 0.45);
        vec3 sc = mix(u_baseColor*0.80, u_baseColor*1.55+vec3(0.12,0.08,0.02), surf)*diff;
        vec3 rc = u_glowColor*(2.0+u_pullT*1.8);
        finalColor  = mix(sc,rc,rimAtm*0.55)+rc*rimCore;
        finalAlpha  = 1.0;

    // ── STAR REMNANT (type 2) ─────────────────────────────────────────────────
    } else if (u_type < 2.5) {
        float tbl = mix(fbm(rp*2.2+u_time*0.030), fbm(rp*4.8-u_time*0.018), 0.4);
        vec3 sc = mix(vec3(1.0,0.98,0.85), mix(vec3(1.0,0.65,0.16), u_baseColor*0.9, rim*0.6), tbl)*(1.5+tbl*0.4+u_pullT*0.5);
        finalColor  = sc + u_glowColor*(3.2+u_pullT*2.5)*rimCore*2.5;
        finalAlpha  = 1.0;

    // ── PLANET (type 3) – guaranteed bright surfaces ──────────────────────────
    } else {
        // ── INITIALIZE with Ocean defaults (safe fallback) ────────────────────
        vec3 surfA = vec3(0.14, 0.42, 0.88);   // primary surface color
        vec3 surfB = vec3(0.86, 0.93, 0.99);   // secondary / bright highlights
        vec3 atm   = vec3(0.40, 0.72, 1.00);

        float arch = floor(u_seed * 5.0);

        // Sequential ifs (not else-if) – avoids uninitialized variable UB on mobile GPUs
        if (arch > 0.5) { surfA=vec3(0.18,0.58,0.22); surfB=vec3(0.62,0.90,0.46); atm=vec3(0.35,1.00,0.50); } // Jungle
        if (arch > 1.5) { surfA=vec3(0.88,0.60,0.18); surfB=vec3(1.00,0.90,0.62); atm=vec3(1.00,0.74,0.28); } // Desert
        if (arch > 2.5) { surfA=vec3(0.62,0.80,0.97); surfB=vec3(0.96,0.98,1.00); atm=vec3(0.72,0.92,1.00); } // Ice
        if (arch > 3.5) { surfA=vec3(0.84,0.18,0.04); surfB=vec3(1.00,0.65,0.10); atm=vec3(1.00,0.40,0.08); } // Lava

        // Surface texture – blend between two bright colors
        float n1   = fbm(rp*2.0 + u_seed);
        float n2   = fbm(rp*4.5 + 0.5 + u_seed*2.0);
        float texW = clamp(mix(n1,n2,0.35)*0.55 + 0.22, 0.0, 1.0);  // 0.22..0.77

        vec3 surfCol = mix(surfA, surfB, texW);

        // Cloud overlay
        float clouds = fbm(rp*3.5 + vec3(u_time*0.04, 0.0, u_seed*1.5));
        surfCol = mix(surfCol, vec3(0.92,0.95,0.98), smoothstep(0.45,0.62,clouds)*0.45);

        // Apply lighting (guaranteed >= 0.70 ambient)
        surfCol *= diff;

        // Atmosphere halo
        vec3 atmosphere = atm * (2.6 + u_pullT * 0.8);
        finalColor = surfCol + atmosphere*rimAtm*1.2 + atmosphere*rimCore*2.2;
        finalAlpha = 1.0;
    }

    // Absolute minimum – no pixel can be true black
    finalColor = max(finalColor, vec3(0.07, 0.05, 0.03));

    // Tidal distortion near BH
    if (u_pullT > 0.05) {
        finalColor = mix(finalColor, vec3(0.7,0.2,1.0)*length(finalColor), u_pullT*0.22);
    }

    if (finalAlpha < 0.008) discard;
    gl_FragColor = vec4(finalColor, finalAlpha);
}
