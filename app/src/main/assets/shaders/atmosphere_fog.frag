#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform float u_surfaceDist;
uniform float u_fogStart;
uniform float u_time;
uniform float u_alphaScale;
uniform float u_cloudStrength;

float hash(vec2 p){p=fract(p*vec2(123.34,456.21));p+=dot(p,p+45.32);return fract(p.x*p.y);}
float vnoise(vec2 p){
    vec2 i=floor(p); vec2 f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1.0,0.0)),f.x),mix(hash(i+vec2(0.0,1.0)),hash(i+vec2(1.0,1.0)),f.x),f.y);
}
float fbm(vec2 p){return vnoise(p)*0.52+vnoise(p*2.1+4.7)*0.30+vnoise(p*4.2+8.1)*0.18;}

void main() {
    if (u_surfaceDist > u_fogStart) discard;

    float enter = 1.0 - clamp(u_surfaceDist / u_fogStart, 0.0, 1.0);
    enter = smoothstep(0.0, 1.0, enter);
    float deep  = 1.0 - clamp(u_surfaceDist / 360.0, 0.0, 1.0);
    float baseA = enter * 0.075 + deep * 0.065;

    float skyT = v_uv.y;
    float horizon = 1.0 - abs(skyT - 0.48) * 1.55;
    horizon = clamp(horizon, 0.18, 1.0);
    float a = baseA * horizon * (1.0 - skyT * 0.36) * u_alphaScale;
    if (a < 0.003) discard;

    vec3 col = mix(vec3(0.20, 0.55, 0.88), vec3(0.08, 0.22, 0.48), skyT * 0.45);

    if (u_cloudStrength > 0.01) {
        float cloudWindow = (1.0 - smoothstep(520.0, 900.0, u_surfaceDist)) * smoothstep(20.0, 120.0, u_surfaceDist);
        vec2 windUv = v_uv * vec2(4.2, 2.2) + vec2(u_time * 0.045, -u_time * 0.018);
        float cloud = fbm(windUv);
        cloud = smoothstep(0.48, 0.72, cloud) * smoothstep(0.02, 0.28, v_uv.y) * (1.0 - smoothstep(0.68, 1.0, v_uv.y));
        float cloudA = cloud * cloudWindow * 0.30 * u_cloudStrength;
        col = mix(col, vec3(0.88, 0.94, 0.98), cloudA);
        a += cloudA;
    }

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    col += (dither - 0.5) / 255.0;

    gl_FragColor = vec4(col, a);
}
