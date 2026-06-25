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
uniform float u_detail;

float hash(vec3 p){p=fract(p*vec3(443.897,441.423,437.195));p+=dot(p,p.yzx+19.19);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p);vec3 f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){return vnoise(p)*0.62+vnoise(p*2.1+0.73)*0.38;}
float fbm4(vec3 p){
    return fbm(p)*0.50 + fbm(p*2.03+1.7)*0.28 + fbm(p*4.07+3.1)*0.14 + fbm(p*8.13+5.3)*0.08;
}
vec3 warp(vec3 p){
    return p + vec3(fbm(p*1.4+1.9), fbm(p*1.5+3.2), fbm(p*1.6+5.1)) * 0.22;
}

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

    // ── PLANET (type 3) ───────────────────────────────────────────────────────
    } else {
        vec3 surfA = vec3(0.05, 0.20, 0.55);
        vec3 surfB = vec3(0.16, 0.44, 0.78);
        vec3 atm   = vec3(0.30, 0.58, 0.90);

        float arch = floor(u_seed * 5.0);

        if (arch > 0.5) { surfA=vec3(0.10,0.42,0.16); surfB=vec3(0.28,0.62,0.30); atm=vec3(0.25,0.72,0.38); }
        if (arch > 1.5) { surfA=vec3(0.62,0.42,0.14); surfB=vec3(0.82,0.68,0.28); atm=vec3(0.85,0.58,0.22); }
        if (arch > 2.5) { surfA=vec3(0.48,0.62,0.82); surfB=vec3(0.72,0.82,0.92); atm=vec3(0.55,0.78,0.95); }
        if (arch > 3.5) { surfA=vec3(0.58,0.12,0.04); surfB=vec3(0.82,0.32,0.08); atm=vec3(0.85,0.32,0.10); }

        float detailT = clamp(u_detail, 0.0, 1.0);
        vec3 wp = (detailT > 0.03) ? mix(rp, warp(rp), detailT) : rp;
        vec3 surfCol;

        // ── Ocean (arch 0) ────────────────────────────────────────────────────
        if (arch < 0.5) {
            float depthLow = fbm(wp * 1.6 + u_seed * 3.0);
            float depth;
            if (detailT > 0.03) {
                float depthHigh = fbm4(wp * 1.5 + u_seed * 3.0);
                depthHigh = mix(depthHigh, fbm4(wp * 2.8 + 2.4), 0.35);
                depth = mix(depthLow, depthHigh, smoothstep(0.12, 0.85, detailT));
            } else {
                depth = depthLow;
            }
            float shallow = smoothstep(0.30, 0.75, depth);
            vec3 deepW  = vec3(0.03, 0.12, 0.38);
            vec3 midW   = vec3(0.08, 0.30, 0.62);
            vec3 shelfW = vec3(0.14, 0.42, 0.72);
            surfCol = mix(deepW, midW, smoothstep(0.0, 0.55, shallow));
            surfCol = mix(surfCol, shelfW, smoothstep(0.55, 1.0, shallow) * 0.65);
            float cloudsLow = fbm(wp * 2.0 + u_seed * 1.5);
            float clouds = cloudsLow;
            if (detailT > 0.03) {
                float cloudsHigh = fbm4(wp * 2.0 + vec3(u_seed * 2.0, 1.8, 0.6));
                clouds = mix(cloudsLow, cloudsHigh, smoothstep(0.18, 0.90, detailT));
            }
            surfCol = mix(surfCol, vec3(0.50, 0.62, 0.74), smoothstep(0.56, 0.80, clouds) * (0.10 + detailT * 0.12));

            vec3 waveP = rp * (70.0 + detailT * 680.0) + vec3(u_time * 0.10, 0.0, -u_time * 0.075);
            float waveA = fbm(waveP);
            float waveB = fbm(waveP * 1.9 + vec3(3.1, u_time * 0.05, 1.7));
            float streaks = smoothstep(0.58, 0.86, waveA) * smoothstep(0.42, 0.78, waveB);
            float foam = streaks * detailT * max(0.0, dot(n, vdir)) * 0.32;
            surfCol = mix(surfCol, vec3(0.42, 0.68, 0.88), foam);
            surfCol += vec3(0.04, 0.10, 0.15) * streaks * detailT;
        } else if (arch < 1.5) {
            vec3 earthP = wp * 2.2 + vec3(u_seed * 4.0, 0.7, 1.3);
            float continent = fbm4(earthP);
            continent = mix(continent, fbm4(earthP * 1.9 + 3.4), 0.28);
            float land = smoothstep(0.47, 0.56, continent);
            float coast = smoothstep(0.44, 0.52, continent) * (1.0 - smoothstep(0.55, 0.63, continent));

            float terrain = fbm4(wp * 6.0 + 2.7);
            vec3 oceanDeep = vec3(0.025, 0.13, 0.38);
            vec3 oceanShelf = vec3(0.06, 0.34, 0.58);
            vec3 grass = vec3(0.10, 0.36, 0.13);
            vec3 forest = vec3(0.05, 0.22, 0.08);
            vec3 highland = vec3(0.42, 0.34, 0.18);
            vec3 beach = vec3(0.63, 0.56, 0.34);

            vec3 water = mix(oceanDeep, oceanShelf, coast * 0.85);
            vec3 landCol = mix(forest, grass, smoothstep(0.22, 0.72, terrain));
            landCol = mix(landCol, highland, smoothstep(0.62, 0.88, terrain) * 0.55);
            surfCol = mix(water, landCol, land);
            surfCol = mix(surfCol, beach, coast * 0.50);

            float clouds = fbm4(wp * 3.0 + vec3(u_time * 0.018, 2.1, -u_time * 0.012));
            clouds = smoothstep(0.56, 0.78, clouds);
            surfCol = mix(surfCol, vec3(0.80, 0.86, 0.88), clouds * (0.12 + detailT * 0.13));
            atm = vec3(0.34, 0.62, 0.92);
        } else {
            float n1 = fbm4(wp * 1.8 + u_seed);
            float n2 = fbm4(wp * 3.2 + 0.5 + u_seed * 2.0);
            float texW = clamp(mix(n1, n2, 0.40) * 0.50 + 0.25, 0.0, 1.0);
            surfCol = mix(surfA, surfB, texW);
            float clouds = fbm4(wp * 2.4 + vec3(u_seed * 1.5, 2.1, 0.8));
            surfCol = mix(surfCol, vec3(0.72,0.78,0.85), smoothstep(0.52,0.74,clouds)*0.24);
        }

        vec3 lightDir = normalize(vec3(0.55, 0.85, -0.25));
        float sunLit  = max(0.0, dot(n, lightDir));
        float diff    = sunLit * 0.42 + 0.36;
        surfCol *= diff;

        // Sun glint on water (high detail only)
        if (arch < 0.5 && detailT > 0.03) {
            vec3 halfV = normalize(lightDir + vdir);
            float spec = pow(max(0.0, dot(n, halfV)), 48.0) * sunLit * 0.62 * detailT;
            surfCol += vec3(0.65, 0.82, 1.0) * spec;
        }

        float facing  = max(0.0, dot(n, vdir));
        float atmRing = smoothstep(0.58, 0.94, rim) * (1.0 - facing * 0.35);
        float atmHot  = pow(max(0.0, rim - 0.72), 4.0);
        finalColor = surfCol + atm * (atmRing * 0.28 + atmHot * 0.55);
        finalAlpha = 1.0;
    }

    // Soft clamp — no HDR white blowout on mobile displays
    finalColor = clamp(finalColor, vec3(0.02, 0.02, 0.03), vec3(0.92));

    // Tidal distortion near BH
    if (u_pullT > 0.05) {
        finalColor = mix(finalColor, vec3(0.7,0.2,1.0)*length(finalColor), u_pullT*0.22);
    }

    if (finalAlpha < 0.008) discard;
    gl_FragColor = vec4(finalColor, finalAlpha);
}
