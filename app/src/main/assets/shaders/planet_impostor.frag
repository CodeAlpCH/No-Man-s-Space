#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform vec3 u_oceanDeep;
uniform vec3 u_oceanShallow;
uniform vec3 u_atmColor;
uniform float u_seed;

float hash(vec2 p){p=fract(p*vec2(123.34,345.45));p+=dot(p,p+34.23);return fract(p.x*p.y);}
float vnoise(vec2 p){
    vec2 i=floor(p); vec2 f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1.0,0.0)),f.x),mix(hash(i+vec2(0.0,1.0)),hash(i+vec2(1.0,1.0)),f.x),f.y);
}
float fbm(vec2 p){return vnoise(p)*0.55+vnoise(p*2.1+1.7)*0.30+vnoise(p*4.2+3.9)*0.15;}

float blob(vec2 p, vec2 c, vec2 s) {
    vec2 d = (p - c) / s;
    return exp(-dot(d, d));
}

void main() {
    vec2 p = v_uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.0) discard;

    float rim = smoothstep(0.70, 0.98, r);
    float arch = floor(u_seed * 5.0);
    vec3 col = mix(u_oceanDeep, u_oceanShallow, 1.0 - r * 0.80);
    if (arch > 0.5 && arch < 1.5) {
        float continents =
            blob(p, vec2(-0.46,  0.18), vec2(0.34, 0.46)) +
            blob(p, vec2( 0.18,  0.30), vec2(0.42, 0.30)) +
            blob(p, vec2( 0.43, -0.30), vec2(0.28, 0.38)) +
            blob(p, vec2(-0.12, -0.48), vec2(0.24, 0.28));
        continents += (fbm(p * 4.0 + vec2(u_seed * 7.0, 1.8)) - 0.5) * 0.62;
        float land = smoothstep(0.42, 0.55, continents);
        float coast = smoothstep(0.34, 0.48, continents) * (1.0 - smoothstep(0.56, 0.70, continents));
        vec3 water = mix(vec3(0.02, 0.13, 0.40), vec3(0.04, 0.34, 0.68), coast);
        vec3 landCol = mix(vec3(0.08, 0.42, 0.13), vec3(0.48, 0.36, 0.18), fbm(p * 8.0 + 2.0) * 0.62);
        col = mix(water, landCol, land);
        col = mix(col, vec3(0.76, 0.70, 0.42), coast * 0.58);

        float clouds = fbm(p * vec2(5.8, 2.4) + vec2(2.6, 0.4));
        clouds = smoothstep(0.57, 0.77, clouds) * (1.0 - r * 0.28);
        col = mix(col, vec3(0.88, 0.94, 0.98), clouds * 0.32);
    }
    col = mix(col, u_atmColor, rim * 0.50);

    float alpha = smoothstep(1.0, 0.90, r);
    gl_FragColor = vec4(col, alpha);
}
