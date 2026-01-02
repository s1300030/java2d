import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.io.*;


public class Project01{
    public static void main(String[] args){

        // == 画面設定 Setting Window ==
        JFrame f = new JFrame("Project01");

        int w = 700;
        int h = 700;

         // == .vertファイル読み込み .vert loading==
        java.util.List<java.util.List<Point2D.Double>> curves = null;
        if(args[0]!=null){
            try {
                curves = VertLoader.load(args[0]);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        else
        {
            System.out.println("Please input fileName (% java Project01 vert/key.vert)");
            System.exit(1);
        }

        DrawPanel dp = new DrawPanel(w, h, curves);
        f.add(dp, BorderLayout.CENTER);

        f.pack();
        f.setSize(w, h);
	    f.setVisible(true);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
	    });
        f.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '0') {
                    dp.ChangeViewMode(0);
                }
                
                else if (e.getKeyChar() == '1') {
                    dp.ChangeViewMode(1);   // Tangent
                }

                else if (e.getKeyChar() == '2') {
                    dp.ChangeViewMode(2);   // Normal
                }

                else if (e.getKeyChar() == '3') {
                    dp.ChangeViewMode(3);  // Curvature
                }

                else if (e.getKeyChar() == '4') {
                    dp.evolveOnce();    // Evolve
                }
            }
        });

    }
}

class DrawPanel extends JPanel {

    int w,h;
    java.util.List<java.util.List<Point2D.Double>> curves;

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    double scale;
    int margin = 5; //空白

    ViewMode viewMode = ViewMode.Default;
    
    public DrawPanel(int w, int h, java.util.List<java.util.List<Point2D.Double>> curves) {
        
        this.w = w;
        this.h = h;
        this.curves = curves;

        // Window Scaling
        Scaling();
    }

    public void paint(Graphics g){
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3));

        for (java.util.List<Point2D.Double> vertices : curves) {

            int n = vertices.size();
            for (int i = 0; i < n; i++) {
                Point2D.Double p1 = vertices.get(i);
                Point2D.Double p2 = vertices.get((i + 1) % n);

                Point s1 = toScreen(p1);
                Point s2 = toScreen(p2);
                g2.drawLine(s1.x, s1.y, s2.x, s2.y);
            }
        }

        // 1. Unit Tangent
        if(viewMode == ViewMode.Tangent){
            double arrowLen = 20.0;
            for (java.util.List<Point2D.Double> vertices : curves) {
                int n = vertices.size();
                for (int i = 0; i < n; i++) {
                    Point2D.Double p = vertices.get(i);
                    Point screenP = toScreen(p);

                    Point2D.Double T = computeTangent(vertices, i);

                    int tx = (int)(T.x * arrowLen);
                    int ty = (int)(T.y * arrowLen);

                    g2.setColor(Color.BLUE);
                    g2.drawLine(screenP.x, screenP.y,
                                screenP.x + tx,
                                screenP.y - ty);
                }
            }
        }

        // 2. Unit Normal
        else if(viewMode == ViewMode.Normal){
            double arrowLen = 20.0;
            for (java.util.List<Point2D.Double> vertices : curves) {
                int n = vertices.size();
                for (int i = 0; i < n; i++) {
                    Point2D.Double p = vertices.get(i);
                    Point screenP = toScreen(p);

                    Point2D.Double T = computeTangent(vertices, i);
                    Point2D.Double N = computeNormal(T);

                    int nx = (int)(N.x * arrowLen);
                    int ny = (int)(N.y * arrowLen);

                    g2.setColor(Color.GREEN);
                    g2.drawLine(screenP.x, screenP.y,
                                screenP.x + nx,
                                screenP.y - ny); 
                }
            }
        }

        // 3. Curvature
        else if(viewMode == ViewMode.Curvature){
            double scaleLen = 25.0;

            for (java.util.List<Point2D.Double> vertices : curves) {
                int n = vertices.size();
                for (int i = 0; i < n; i++) {
                    Point2D.Double p = vertices.get(i);
                    Point screenP = toScreen(p);

                    Point2D.Double T = computeTangent(vertices, i);
                    Point2D.Double N = computeNormal(T);
                    double ki = computeCurvature(vertices, i);

                    double len = ki * scaleLen;

                    int nx = (int)(N.x * len);
                    int ny = (int)(N.y * len);

                    g2.setColor(Color.ORANGE);
                    g2.drawLine(screenP.x, screenP.y,
                                screenP.x + nx,
                                screenP.y - ny);
                }
            }
        }

    }


    public void ChangeViewMode(int x){

        switch (x) {
            case 1:
                viewMode = ViewMode.Tangent;
                break;

            case 2:
                viewMode = ViewMode.Normal;
                break;
            
            case 3:
                viewMode = ViewMode.Curvature;
                break;

            case 0:
            default:
                viewMode = ViewMode.Default;
                break;
        }

        repaint();

    }

    public void evolveOnce() {
        double dt = 0.01;
        evolveByCurvatureFlow(dt);
        Scaling();
        repaint();
    }

    private void Scaling(){
        
        minX = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        for (var comp : curves) {
            for (Point2D.Double p : comp) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }
        double width  = maxX - minX;
        double height = maxY - minY;

        if (width == 0) width = 1;
        if (height == 0) height = 1;

        double sx = (w * 0.9 - 2.0 * (double)margin) / width;
        double sy = (h * 0.9 - 2.0 * (double)margin) / height;

        scale = 0.9 * Math.min(sx, sy);
    }

    private Point toScreen(Point2D.Double p) {
        int sx = (int)((getWidth() - (maxX - minX) * scale) / 2.0 + (p.x - minX) * scale);
        int sy = (int)(getHeight() - (getHeight() - (maxY - minY) * scale) / 2.0 - (p.y - minY) * scale);

        return new Point(sx, sy);
    }

    private Point2D.Double computeTangent(java.util.List<Point2D.Double> comp, int idx){
        int n = comp.size();
        Point2D.Double prev = comp.get((idx - 1 + n) % n);
        Point2D.Double next = comp.get((idx + 1) % n);

        double vx = next.x - prev.x;
        double vy = next.y - prev.y;
        double len = Math.hypot(vx, vy);
        if (len == 0) return new Point2D.Double(0, 0);

        return new Point2D.Double(vx / len, vy / len);
    }

    private Point2D.Double computeNormal(Point2D.Double T){
        return new Point2D.Double(-T.y, T.x);
    }

    private double computeCurvature(java.util.List<Point2D.Double> comp, int idx){
        int n = comp.size();
        Point2D.Double prev = comp.get((idx - 1 + n) % n);
        Point2D.Double p    = comp.get(idx);
        Point2D.Double next = comp.get((idx + 1) % n);

        // a = p - prev, b = next - p
        double ax = p.x    - prev.x;
        double ay = p.y    - prev.y;
        double bx = next.x - p.x;
        double by = next.y - p.y;

        double la = Math.hypot(ax, ay);
        double lb = Math.hypot(bx, by);

        if (la == 0 || lb == 0) return 0.0;

        double dot = (ax * bx + ay * by) / (la * lb);
        if (dot > 1.0)  dot = 1.0;
        if (dot < -1.0) dot = -1.0;

        double theta = Math.acos(dot);

        // chord length d = |next - prev|
        double dx = next.x - prev.x;
        double dy = next.y - prev.y;
        double d  = Math.hypot(dx, dy);
        if (d == 0) return 0.0;

        double ki = 2.0 * Math.sin(theta) / d;
        double cross = ax * by - ay * bx; // a×b
        if (cross < 0) ki = -ki;

        return ki;
    }

    private void evolveByCurvatureFlow(double dt) {

        for (int c = 0; c < curves.size(); ++c) {
            java.util.List<Point2D.Double> comp = curves.get(c);
            int n = comp.size();
            java.util.List<Point2D.Double> newComp = new java.util.ArrayList<>();

            for (int i = 0; i < n; ++i) {
                Point2D.Double p = comp.get(i);

                Point2D.Double T = computeTangent(comp, i);
                Point2D.Double N = computeNormal(T);
                double kappa = computeCurvature(comp, i);

                double newX = p.x + dt * kappa * N.x;
                double newY = p.y + dt * kappa * N.y;

                newComp.add(new Point2D.Double(newX, newY));
            }

            comp.clear();
            comp.addAll(newComp);
        }
    }

}

class VertLoader {

    public static java.util.List<java.util.List<Point2D.Double>> load(String path) throws IOException {

        java.util.List<java.util.List<Point2D.Double>> components = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            // == Number of Components 線の数 ==
            String line = br.readLine();
            if (line == null) throw new IOException("Empty file: " + path);
            int compCount = Integer.parseInt(line.trim());
            System.out.println("components = " + compCount);

            for (int c = 0; c < compCount; c++) {

                // == Number of Vertices 頂点数 ==
                line = br.readLine();
                if (line == null) throw new IOException("Unexpected EOF when reading vertex count");
                int n = Integer.parseInt(line.trim());
                System.out.println("n = " + n);

                java.util.List<Point2D.Double> vertices = new java.util.ArrayList<>();

                for (int i = 0; i < n; i++) {
                    line = br.readLine();
                    //System.out.println("i = " + i);
                    if (line == null) throw new IOException("Unexpected EOF when reading vertices");

                    String[] sp = line.trim().split("\\s+");
                    double x = Double.parseDouble(sp[0]);
                    double y = Double.parseDouble(sp[1]);
                    vertices.add(new Point2D.Double(x, y));
                }

                components.add(vertices);
            }
        }

        return components;
    }
}

enum ViewMode{
    Default, Tangent, Normal, Curvature
}

