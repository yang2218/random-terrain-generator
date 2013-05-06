import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

public class objReader extends JFrame implements MouseListener, MouseMotionListener, GLEventListener, KeyListener {

    class objModel {

        public FloatBuffer vertexBuffer;
        public IntBuffer faceBuffer;
        public FloatBuffer normalBuffer;
        public Point3f center;
        public int num_verts;           // number of vertices
        public int num_faces;           // number of triangle faces
        public float objHeight;
        
        int[] texture;
        
        public void Draw() {
            vertexBuffer.rewind();
            normalBuffer.rewind();
            faceBuffer.rewind();
            
            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

            gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
            gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);

            gl.glDrawElements(GL.GL_TRIANGLES, num_faces * 3, GL.GL_UNSIGNED_INT, faceBuffer);

            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
            
        }


        public void init(GL gl) {
            
            gl.glGenTextures(1, texture, 0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
            ByteBuffer b = genTexture(32);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, 32, 32, 0, GL.GL_RGB, GL.GL_BYTE, b);
        }
        
        private ByteBuffer genTexture(int size) {
            ByteBuffer b = BufferUtil.newByteBuffer(size * size * 3);
            byte[] b2 = new byte[size * size * 3];
            //Random r = new Random();
            //r.nextBytes(b2);
            
            for(int i = 0; i < b2.length; i += 3){
            	
            	int r1 = 0;
            	int g1 = 91;
            	int bl1 = 0;
            	int r2 = 99;
            	int g2 = 49;
            	int bl2 = 14;
            	
            	double weight = Math.random();
            	
            	int texR = (int)((weight*r1) + (1-weight)*(r2)); 
            	int texG = (int)((weight*g1) + (1-weight)*(g2));
            	int texB = (int)((weight*bl1) + (1-weight)*(bl2));
            	
            	Integer r = new Integer(texR);
            	Integer g = new Integer(texG);
            	Integer bl = new Integer(texB);
            	
            	b2[i] = r.byteValue();
            	b2[i+1] = g.byteValue();
            	b2[i+2] = bl.byteValue();
            	
            }
            
            b.put(b2);
            b.flip();
            return b;
        }
        
        public objModel(String filename) {
        	
        	texture = new int[1];
        	
            /* load a triangular mesh model from a .obj file */
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(filename));
            } catch (IOException e) {
                System.out.println("Error reading from file " + filename);
                System.exit(0);
            }

            center = new Point3f();
            float x, y, z;
            int v1, v2, v3;
            float minx, miny, minz;
            float maxx, maxy, maxz;
            float bbx,bby, bbz;
            minx = miny = minz = 10000.f;
            maxx = maxy = maxz = -10000.f;

            String line;
            String[] tokens;
            ArrayList<Point3f> input_verts = new ArrayList<Point3f>();
            ArrayList<Integer> input_faces = new ArrayList<Integer>();
            ArrayList<Vector3f> input_norms = new ArrayList<Vector3f>();
            try {
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) {
                        continue;
                    }
                    switch (line.charAt(0)) {
                        case 'v':
                            tokens = line.split("[ ]+");
                            x = Float.valueOf(tokens[1]);
                            y = Float.valueOf(tokens[2]);
                            z = Float.valueOf(tokens[3]);
                            minx = Math.min(minx, x);
                            miny = Math.min(miny, y);
                            minz = Math.min(minz, z);
                            maxx = Math.max(maxx, x);
                            maxy = Math.max(maxy, y);
                            maxz = Math.max(maxz, z);
                            input_verts.add(new Point3f(x, y, z));
                            center.add(new Point3f(x, y, z));
                            break;
                        case 'f':
                            tokens = line.split("[ ]+");
                            v1 = Integer.valueOf(tokens[1]) - 1;
                            v2 = Integer.valueOf(tokens[2]) - 1;
                            v3 = Integer.valueOf(tokens[3]) - 1;
                            input_faces.add(v1);
                            input_faces.add(v2);
                            input_faces.add(v3);
                            break;
                        default:
                            continue;
                    }
                }
                in.close();
            } catch (IOException e) {
                System.out.println("Unhandled error while reading input file.");
            }

            System.out.println("Read " + input_verts.size()
                    + " vertices and " + input_faces.size() + " faces.");

            center.scale(1.f / (float) input_verts.size());

            bbx = maxx - minx;
            bby = maxy - miny;
            bbz = maxz - minz;
            float bbmax = Math.max(bbx, Math.max(bby, bbz));
            objHeight = Math.abs((miny - center.y)/bby);
            for (Point3f p : input_verts) {

                p.x = (p.x - center.x) / bbmax;
                p.y = (p.y - center.y) / bbmax;
                p.z = (p.z - center.z) / bbmax;                
            }
            center.x = center.y = center.z = 0.f;

            /* estimate per vertex average normal */
            int i;
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.add(new Vector3f());
            }

            Vector3f e1 = new Vector3f();
            Vector3f e2 = new Vector3f();
            Vector3f tn = new Vector3f();
            for (i = 0; i < input_faces.size(); i += 3) {
                v1 = input_faces.get(i + 0);
                v2 = input_faces.get(i + 1);
                v3 = input_faces.get(i + 2);

                e1.sub(input_verts.get(v2), input_verts.get(v1));
                e2.sub(input_verts.get(v3), input_verts.get(v1));
                tn.cross(e1, e2);
                input_norms.get(v1).add(tn);

                e1.sub(input_verts.get(v3), input_verts.get(v2));
                e2.sub(input_verts.get(v1), input_verts.get(v2));
                tn.cross(e1, e2);
                input_norms.get(v2).add(tn);

                e1.sub(input_verts.get(v1), input_verts.get(v3));
                e2.sub(input_verts.get(v2), input_verts.get(v3));
                tn.cross(e1, e2);
                input_norms.get(v3).add(tn);
            }

            /* convert to buffers to improve display speed */
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.get(i).normalize();
            }

            vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size() * 3);
            normalBuffer = BufferUtil.newFloatBuffer(input_verts.size() * 3);
            faceBuffer = BufferUtil.newIntBuffer(input_faces.size());

            for (i = 0; i < input_verts.size(); i++) {
                vertexBuffer.put(input_verts.get(i).x);
                vertexBuffer.put(input_verts.get(i).y);
                vertexBuffer.put(input_verts.get(i).z);
                normalBuffer.put(input_norms.get(i).x);
                normalBuffer.put(input_norms.get(i).y);
                normalBuffer.put(input_norms.get(i).z);
            }

            for (i = 0; i < input_faces.size(); i++) {
                faceBuffer.put(input_faces.get(i));
            }
            num_verts = input_verts.size();
            num_faces = input_faces.size() / 3;
        }
    }
    //models to be selected from
    private objModel tree_aspen = new objModel("tree_aspen.obj");
    private objModel tree_conical = new objModel("tree_conical.obj");
    private objModel tulip = new objModel("tulip.obj");
    private objModel cactus = new objModel("cactus.obj");
    private objModel plant = new objModel("plant.obj");
    private objModel statue = new objModel("statue.obj");
    private ArrayList<objPosition> sceneObjects;
    //scene rendering
    private final GLCanvas canvas;
    private GL gl;
    private final GLU glu = new GLU();
    private FPSAnimator animator;
    private int winW = 800, winH = 800;
    private boolean wireframe = false;
    private boolean cullface = true;
    private boolean flatshade = false;
    private float xpos = 0, ypos = 0, zpos = 0;
    private float centerx, centery, centerz;
    private float roth = 0, rotv = 0;
    private float znear, zfar;
    private int mouseX, mouseY, mouseButton;
    private float motionSpeed, rotateSpeed;
    private float animation_speed = 1.0f;
    TerrainGenerator terrain;
    int terrainDim = 512;
    float terrainSharpness = 3f;
    float terrainSize = 30f;
    Vector3f[][] points;
    private int numberOfObjects = (int) (Math.pow(terrainSize, 2) * Math.random()*0.1f);
    private boolean showTerrain = false;
    private float xmin = -4f, ymin = -4f, zmin = -4f;
    private float xmax = 4f, ymax = 4f, zmax = 4f;
    
    private int[] textures;

    public objReader() {
        super("Procedural Terrain Gen");
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        animator = new FPSAnimator(canvas, 30); // create a 30 fps animator
        getContentPane().add(canvas);
        setSize(winW, winH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        animator.start();
        canvas.requestFocus();
    }

    void initViewParameters() {
        roth = rotv = 0;

        float ball_r = (float) Math.sqrt((xmax - xmin) * (xmax - xmin)
                + (ymax - ymin) * (ymax - ymin)
                + (zmax - zmin) * (zmax - zmin)) * 0.707f;

        centerx = (xmax + xmin) / 2.f;
        centery = (ymax + ymin) / 2.f;
        centerz = (zmax + zmin) / 2.f;
        xpos = centerx;
        ypos = centery;
        zpos = ball_r / (float) Math.sin(45.f * Math.PI / 180.f) + centerz;

        znear = 0.01f;
        zfar = 1000.f;

        motionSpeed = 0.002f * ball_r;
        rotateSpeed = 0.1f;

    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        winW = width;
        winH = height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.f, (float) width / (float) height, znear, zfar);
        gl.glMatrixMode(GL.GL_MODELVIEW);
    }

    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL();

        initViewParameters();
        gl.glClearColor(.3f, .3f, .8f, 1f);
        gl.glClearDepth(1.0f);

        // white light at the eye
        float light0_position[] = {0, 0, 1, 0};
        float light0_diffuse[] = {1, 1, 1, 1};
        float light0_specular[] = {1, 1, 1, 1};
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);

        //red light
        float light1_position[] = {-.1f, .1f, 0, 0};
        float light1_diffuse[] = {.75f, .05f, .05f, 1};
        float light1_specular[] = {.75f, .05f, .05f, 1};
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

        //blue light
        float light2_position[] = {.1f, .1f, 0, 0};
        float light2_diffuse[] = {.05f, .05f, .75f, 1};
        float light2_specular[] = {.05f, .05f, .75f, 1};
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);

        //material
        float mat_ambient[] = {0, 0, 0, 1};
        float mat_specular[] = {.8f, .8f, .8f, 1};
        float mat_diffuse[] = {.4f, .4f, .4f, 1};
        float mat_shininess[] = {128};
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

        float bmat_ambient[] = {0, 0, 0, 1};
        float bmat_specular[] = {0, .8f, .8f, 1};
        float bmat_diffuse[] = {0, .4f, .4f, 1};
        float bmat_shininess[] = {128};
        gl.glMaterialfv(GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);

        float lmodel_ambient[] = {0, 0, 0, 1};
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, 1);

        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        gl.glEnable(GL.GL_LIGHT1);
        gl.glEnable(GL.GL_LIGHT2);
        gl.glEnable(GL.GL_LIGHT3);
        gl.glEnable(GL.GL_LIGHT4);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glShadeModel(GL.GL_SMOOTH);

        terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
        points = terrain.points;
        showTerrain = true;

        //create randomly generated object positions
        sceneObjects = new ArrayList<objPosition>();

        tree_aspen.init(gl);
        tree_conical.init(gl);
        statue.init(gl);
        
        //improve to include negative values and possibly alter objPosition
        //to hold an object instead of string (removes switch statement)
        for (int i = 0; i < numberOfObjects; i++) {
            //change this later for more modular random generation
            float x = (float) ((terrainSize*0.5f) * (Math.random()*2 - 1));
            float z = (float) ((terrainSize*0.5f) * (Math.random()*2 - 1));

            float scale = (float) (4 * Math.random());
            int tempRand = (int) (Math.random()*100);
            int objectInt = 0;
            if (0<=tempRand && tempRand<50) {objectInt = 0;}   
            else if (50<=tempRand && tempRand<95) {objectInt = 1;}
            //else if (70<=tempRand && tempRand<85) {objectInt = 2;}
            //else if (85<=tempRand && tempRand<95) {objectInt = 3;}
            else if (95<=tempRand && tempRand<=100) {objectInt = 5;}
            System.out.println(objectInt);

            switch (objectInt) {
                case 0:
                    sceneObjects.add(new objPosition(tree_aspen, x, z, tree_aspen.objHeight));
                    break;
                case 1:
                    sceneObjects.add(new objPosition(tree_conical, x, z, tree_conical.objHeight));
                    break;
                case 2:
                    sceneObjects.add(new objPosition(plant, x, z, plant.objHeight));
                    break;
                case 3:
                    sceneObjects.add(new objPosition(tulip, x, z, tulip.objHeight));
                    break;
                case 4:
                    sceneObjects.add(new objPosition(cactus, x, z, cactus.objHeight));
                    break;
                case 5:
                    sceneObjects.add(new objPosition(statue, x, z, statue.objHeight));                 
                    break;
            }            
        }
        setObjYvals();
        
        loadTexture("texture2.bmp");
    }

    public void resize(){
    	terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
        points = terrain.points;
        showTerrain = true;

        //create randomly generated object positions
        sceneObjects = new ArrayList<objPosition>();

        tree_aspen.init(gl);
        tree_conical.init(gl);
        statue.init(gl);
        
        //improve to include negative values and possibly alter objPosition
        //to hold an object instead of string (removes switch statement)
        for (int i = 0; i < numberOfObjects; i++) {
            //change this later for more modular random generation
            float x = (float) ((terrainSize*0.5f) * (Math.random()*2 - 1));
            float z = (float) ((terrainSize*0.5f) * (Math.random()*2 - 1));

            float scale = (float) (4 * Math.random());
            int tempRand = (int) (Math.random()*100);
            int objectInt = 0;
            if (0<=tempRand && tempRand<50) {objectInt = 0;}   
            else if (50<=tempRand && tempRand<95) {objectInt = 1;}
            //else if (70<=tempRand && tempRand<85) {objectInt = 2;}
            //else if (85<=tempRand && tempRand<95) {objectInt = 3;}
            else if (95<=tempRand && tempRand<=100) {objectInt = 5;}
            System.out.println(objectInt);

            switch (objectInt) {
                case 0:
                    sceneObjects.add(new objPosition(tree_aspen, x, z, tree_aspen.objHeight));
                    break;
                case 1:
                    sceneObjects.add(new objPosition(tree_conical, x, z, tree_conical.objHeight));
                    break;
                case 2:
                    sceneObjects.add(new objPosition(plant, x, z, plant.objHeight));
                    break;
                case 3:
                    sceneObjects.add(new objPosition(tulip, x, z, tulip.objHeight));
                    break;
                case 4:
                    sceneObjects.add(new objPosition(cactus, x, z, cactus.objHeight));
                    break;
                case 5:
                    sceneObjects.add(new objPosition(statue, x, z, statue.objHeight));                 
                    break;
            }            
        }
        setObjYvals();
    }
    
    
    public int loadTexture(String texFile){
    	
    	BufferedImage image = null;
    	
    	try {
			image = ImageIO.read(new File(texFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	textures = new int[image.getWidth()*image.getHeight()];
    	
    	image.getRGB(0, 0, image.getWidth(), image.getHeight(), textures, 0, image.getWidth());
    	
    	return 0;
    }
       
    public static void main(String[] args) {

        new objReader();
    }

    public void display(GLAutoDrawable drawable) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);
        gl.glShadeModel(flatshade ? GL.GL_FLAT : GL.GL_SMOOTH);
        if (cullface) {
            gl.glEnable(GL.GL_CULL_FACE);
        } else {
            gl.glDisable(GL.GL_CULL_FACE);
        }

        gl.glLoadIdentity();

        /* this is the transformation of the entire scene */
        gl.glTranslatef(-xpos, -ypos, -zpos);
        gl.glTranslatef(centerx, centery, centerz);
        gl.glRotatef(360.f - roth, 0, 1.0f, 0);
        gl.glRotatef(rotv, 1.0f, 0, 0);
        gl.glTranslatef(-centerx, -centery, -centerz);

        gl.glEnable(GL.GL_TEXTURE_2D);
        
        
        if (showTerrain) {
            gl.glBegin(GL.GL_QUADS);
            for (int i = 0; i < terrain.dim + 1; i++) {
                for (int j = 0; j < terrain.dim + 1; j++) {
                    if (i != terrain.dim && j != terrain.dim) {
                    	gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
                    	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
                    	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                    	
                        Vector3f normal = new Vector3f();
                        Vector3f tempVec1 = new Vector3f(points[i + 1][j].x - points[i][j].x, points[i + 1][j].y - points[i][j].y, points[i + 1][j].z - points[i][j].z);
                        Vector3f tempVec2 = new Vector3f(points[i][j + 1].x - points[i][j].x, points[i][j + 1].y - points[i][j].y, points[i][j + 1].z - points[i][j].z);
                        normal.cross(tempVec2, tempVec1);
                        gl.glColor3f(Math.abs(points[i][j].y * 0.3f), Math.abs(points[i][j].y * 1f), Math.abs(points[i][j].y * 0.3f));
                        gl.glNormal3f(normal.x, normal.y, normal.z);
                        
                        gl.glTexCoord2f(points[i][j].x, points[i][j].z);
                        gl.glVertex3f(points[i][j].x, points[i][j].y, points[i][j].z);
                        
                        gl.glTexCoord2f(points[i][j + 1].x, points[i][j + 1].z);
                        gl.glVertex3f(points[i][j + 1].x, points[i][j + 1].y, points[i][j + 1].z);
                        
                        gl.glTexCoord2f(points[i + 1][j + 1].x, points[i + 1][j + 1].z);
                        gl.glVertex3f(points[i + 1][j + 1].x, points[i + 1][j + 1].y, points[i + 1][j + 1].z);
                        
                        gl.glTexCoord2f(points[i + 1][j].x, points[i + 1][j].z);
                        gl.glVertex3f(points[i + 1][j].x, points[i + 1][j].y, points[i + 1][j].z);
                    }
                }
            }
            gl.glEnd();
        }
        //simple implementation of random object placement
        for (objPosition o : sceneObjects) {
            gl.glPushMatrix();

            gl.glTranslatef(o.getX(), o.getY(), o.getZ());

            o.getModel().Draw();

            gl.glPopMatrix();
        }

        gl.glDisable(GL.GL_TEXTURE_2D);
        
    }

    public void setObjYvals() {
        for (objPosition o : sceneObjects) {
            float x = o.getX();
            float z = o.getZ();
            float y = 0f;
            float quadDiag = (float) Math.sqrt((2 * Math.pow(terrainSize / terrainDim, 2)));
            for (int i = 0; i < terrain.dim + 1; i++) {
                for (int j = 0; j < terrain.dim + 1; j++) {
                    if (Math.abs(Math.sqrt(Math.pow(points[i][j].x - x, 2) + Math.pow(points[i][j].z - z, 2))) < quadDiag) {
                        o.y = points[i][j].y;
                    }
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        mouseButton = e.getButton();
        canvas.display();
    }

    public void mouseReleased(MouseEvent e) {
        mouseButton = MouseEvent.NOBUTTON;
        canvas.display();
    }

    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (mouseButton == MouseEvent.BUTTON3) {
            zpos -= (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON2) {
            xpos -= (x - mouseX) * motionSpeed;
            ypos += (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON1) {
            roth -= (x - mouseX) * rotateSpeed;
            rotv += (y - mouseY) * rotateSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case KeyEvent.VK_UP:
                terrainSharpness += 0.1f;
                terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
                points = terrain.points;
                setObjYvals();
                System.out.println(terrainSharpness);
                break;
            case KeyEvent.VK_DOWN:
                terrainSharpness -= 0.1f;
                terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
                points = terrain.points;
                setObjYvals();
                System.out.println(terrainSharpness);
                break;
            case 'w':
            case 'W':
                wireframe = !wireframe;
                break;
            case 'b':
            case 'B':
                cullface = !cullface;
                break;
            case '-':
            	terrainSize --;
            	
            	if(terrainSize < 20){
            		terrainDim = 128;
            	}
            	else if(terrainSize >= 20 && terrainSize < 30){
            		terrainDim = 256;
            	}
            	else{
            		terrainDim = 512;
            	}
            	
            	resize();
            	
            	break;
            case '=':
            case KeyEvent.VK_PLUS:
            	terrainSize ++;
            	
            	if(terrainSize < 20){
            		terrainDim = 128;
            	}
            	else if(terrainSize >= 20 && terrainSize < 30){
            		terrainDim = 256;
            	}
            	else{
            		terrainDim = 512;
            	}
            	
            	resize();
            	
            	break;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
    }
}