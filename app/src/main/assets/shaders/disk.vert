attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec2 v_uv;

void main() {
    v_uv        = a_texCoord0;
    gl_Position = u_projViewTrans * u_worldTrans * a_position;
}
