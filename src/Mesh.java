public class Mesh {
    private int vao;
    private int vertices;

    public Mesh(int vao, int vertices) {
        this.vao=vao;
        this.vertices=vertices;
    }
    public int getVaoID() {
		return vao;
	}
	public int getVertexCount() {
		return vertices;
	}
}
