attribute vec4 a_position;
attribute vec3 a_normal;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform vec3 u_camPos;

varying float v_rim;
varying vec3  v_worldPos;

void main() {
    vec4 worldPos = u_worldTrans * a_position;
    v_worldPos    = worldPos.xyz;

    vec3 worldNorm = normalize(mat3(u_worldTrans) * a_normal);
    vec3 viewDir   = normalize(u_camPos - worldPos.xyz);
    v_rim          = 1.0 - max(0.0, dot(worldNorm, viewDir));

    gl_Position = u_projViewTrans * worldPos;
}
