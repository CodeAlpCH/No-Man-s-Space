#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_normal;
varying vec3 v_worldPos;
varying vec3 v_localPos;

uniform vec3  u_camPos;
uniform float u_time;
uniform float u_density;

float hash(vec3 p){p=fract(p*vec3(127.1,311.7,74.7));p+=dot(p,p.yzx+19.19);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p);vec3 f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){
    return vnoise(p)*0.52 + vnoise(p*2.07+2.1)*0.30 + vnoise(p*4.13+4.7)*0.18;
}

void main() {
    vec3 n = normalize(v_normal);
    vec3 vdir = normalize(u_camPos - v_worldPos);
    float facing = max(0.0, dot(n, vdir));
    float rim = 1.0 - facing;

    vec3 p = normalize(v_localPos);
    float drift = u_time * 0.018;
    vec3 bands = vec3(p.x * 6.0 + drift, p.y * 3.2, p.z * 6.0 - drift * 0.7);
    float broad = fbm(bands + vec3(0.0, u_time * 0.012, 1.7));
    float wisps = fbm(bands * 2.7 + vec3(4.1, -u_time * 0.030, 0.3));
    float cloud = smoothstep(0.48, 0.72, broad) * 0.72 + smoothstep(0.56, 0.82, wisps) * 0.42;
    cloud = clamp(cloud, 0.0, 1.0);

    float breakup = smoothstep(0.18, 0.58, fbm(bands * 5.6 + 9.4));
    cloud *= mix(0.42, 1.0, breakup);

    float alpha = cloud * u_density * (0.38 + rim * 0.72);
    if (alpha < 0.018) discard;

    vec3 col = mix(vec3(0.68, 0.78, 0.88), vec3(1.0, 1.0, 0.96), cloud);
    col += vec3(0.16, 0.28, 0.42) * rim;
    gl_FragColor = vec4(col, alpha);
}
