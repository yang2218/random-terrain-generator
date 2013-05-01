
public class objPosition {

	private objReader.objModel objectModel;
	
	//position on the field
	private float x;
	private float y;
	private float z;
	
	//angle related fields
	private float angle;
	private float rot_x;
	private float rot_y;
	private float rot_z;
	
	private float scale;
	
	public objPosition(objReader.objModel model, int rx, int rz){
		
		objectModel = model;
		x = (float)(rx*Math.random());
		z = (float)(rz*Math.random());
	}
	
	public float getX(){
		return x;
	}
	
	public float getZ(){
		return z;
	}
	
	public objReader.objModel getModel(){
		return objectModel;
	}
}
