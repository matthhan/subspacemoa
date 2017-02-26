/**
 * [SubspaceStreamPanel.java] for Subspace MOA
 * 
 * A panel where points and clusters are drawn.
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Timm Jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.gui.subspacevisualization;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.gui.visualization.ClusterPanel;
import moa.gui.visualization.PointPanel;

public class SubspaceStreamPanel extends JPanel implements ComponentListener {

	private static final long serialVersionUID = 1L;

	private boolean debug = false;
	
    private SubspaceClusterPanel highlighted_cluster = null;
    private double zoom_factor = 0.2;
    private int zoom = 1;
    private int width_org;
    private int height_org;
    private int activeXDim = 0;
    private int activeYDim = 1;

    /* Each layer contains only the corresponding visualization */
    private JPanel layerPoints;
    private JPanel layerMicro;
    private JPanel layerMacro;
    private JPanel layerGroundTruth;

    // Buffered image stuffs
    private BufferedImage pointCanvas;
    private pointCanvasPanel layerPointCanvas;
    private boolean pointsVisible = true;
    private boolean ANTIALIAS = false;

    private class pointCanvasPanel extends JPanel {
        
		private static final long serialVersionUID = 1L;
		
		BufferedImage image = null;
        
        public void setImage(BufferedImage image) {
            setSize(image.getWidth(), image.getWidth());
            this.image = image;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            if (image != null)
                g2.drawImage(image, null, 0, 0);
        }
    }


    /** Creates new form SubspaceStreamPanel */
    public SubspaceStreamPanel() {
        initComponents();

        layerPoints = getNewLayer();
        layerPoints.setOpaque(true);
        layerPoints.setBackground(Color.white);
        
        layerMicro = getNewLayer();
        layerMacro = getNewLayer();
        layerGroundTruth = getNewLayer();

        add(layerMacro);
        add(layerMicro);
        add(layerGroundTruth);
        add(layerPoints);

        layerPointCanvas = new pointCanvasPanel();
        add(layerPointCanvas);
        
        addComponentListener(this);
        
    }


    private JPanel getNewLayer() {
        JPanel layer = new JPanel();
        layer.setOpaque(false);
        layer.setLayout(null);
        return layer;
    }


    public void drawMicroClustering(Clustering clustering, Color color) {
        drawClustering(layerMicro, clustering, color);
    }
    
    public void drawMacroClustering(SubspaceClustering clustering, ArrayList<SubspaceDataPoint> points, Color color) {
    	layerMacro.removeAll();
    	
    	List<Cluster> foundClusters = clustering.getClustering();
    	double inclusionProbabilityThreshold = 0.5;
    	for (SubspaceDataPoint p : points) {
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				if (p.getSubspace()[getActiveXDim()] == true || p.getSubspace()[getActiveYDim()] == true) {
    					SubspacePointPanel pointPanel = new SubspacePointPanel(p, this, color);
    			    	layerMacro.add(pointPanel);
    			        pointPanel.updateLocation();
    				}
    			}
    		}
    	}

        if (layerMacro.isVisible() && pointsVisible) {	// Points & Macro together
            Graphics2D imageGraphics = (Graphics2D) pointCanvas.createGraphics();
            imageGraphics.setColor(color);
            drawClusteringsOnCanvas(layerMacro, imageGraphics);
            layerPointCanvas.repaint();
        }

        layerMacro.repaint();
    }
    
    public void drawGTClustering(SubspaceClustering clustering, Color color) {
    	layerGroundTruth.removeAll();
        
        for (int c = 0; c < clustering.size(); c++) {
            Cluster cluster = clustering.get(c);

            SubspaceClusterPanel clusterpanel = new SubspaceClusterPanel(cluster, color, this);
            
            layerGroundTruth.add(clusterpanel);
            clusterpanel.updateLocation();
        }

        if (layerGroundTruth.isVisible() && pointsVisible) {	// Points & GT together
            Graphics2D imageGraphics = (Graphics2D) pointCanvas.createGraphics();
            imageGraphics.setColor(color);
            drawClusteringsOnCanvas(layerGroundTruth, imageGraphics);
            layerPointCanvas.repaint();
        }

        layerGroundTruth.repaint();
    }

    public void setMicroLayerVisibility(boolean visibility) {
        layerMicro.setVisible(visibility);
    }
    public void setMacroLayerVisibility(boolean visibility) {
        layerMacro.setVisible(visibility);
    }
    public void setGroundTruthLayerVisibility(boolean visibility) {
        layerGroundTruth.setVisible(visibility);
    }
    public void setPointVisibility(boolean visibility) {
        pointsVisible = visibility;
        layerPoints.setVisible(visibility);
        if (!visibility)
            layerPointCanvas.setVisible(false);
    }

    /**
     * Draw points on layerPoints (not on pointCanvas buffer). Called for every streamPauseInterval.
     * This method rearranges/redraws the points and removes illusions of clusterings drawn so far.
     * 
     * @param pointarray0
     * @param decay_rate
     * @param decay_threshold
     */
    void drawPointPanels(ArrayList<SubspaceDataPoint> points, double decay_rate, double decay_threshold) {
        for (SubspaceDataPoint p : points) {
            SubspacePointPanel pointPanel = new SubspacePointPanel(p, this, decay_rate, decay_threshold);
            layerPoints.add(pointPanel);
            pointPanel.updateLocation();
        }
        layerPointCanvas.setVisible(false);
        layerPoints.setVisible(pointsVisible);
    }

    /**
     * Draw a point on the stream panel.
     * 
     * @param point
     */
    public void drawPoint(SubspaceDataPoint point, double decay_rate, double decay_threshold) {
        layerPointCanvas.setVisible(pointsVisible);
        layerPoints.setVisible(false);
        if (!pointsVisible)
            return;

        Graphics2D imageGraphics = (Graphics2D) pointCanvas.createGraphics();

        if (ANTIALIAS) {
            imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int size = Math.min(getWidth(), getHeight());
        int x = (int) Math.round(point.value(getActiveXDim()) * size);
        int y = (int) Math.round(point.value(getActiveYDim()) * size);
        int psize = SubspacePointPanel.POINTSIZE;
        int poffset = psize / 2;
        
        /* y-dim color preferred */
        int xDim = getActiveXDim();
        int yDim = getActiveYDim();
        Color c;
        if (point.classValue(xDim) == point.classValue()) {
        	c = SubspacePointPanel.getPointColorbyClass((int)point.classValue(getActiveXDim()), 10);
        } else if (point.classValue(yDim) == point.classValue()) {
        	c = SubspacePointPanel.getPointColorbyClass((int)point.classValue(getActiveYDim()), 10);
        } else {
        	c = Color.GRAY;
        }
        
        imageGraphics.setColor(c);
        if (point.isNoise()) {		// Pure noise
        	imageGraphics.setFont(imageGraphics.getFont().deriveFont(9.0f));
        	imageGraphics.drawChars(new char[] {'x'}, 0, 1, x - poffset, y + poffset);
        } else {
	        imageGraphics.drawOval(x - poffset, y - poffset, psize, psize);
	        imageGraphics.fillOval(x - poffset, y - poffset, psize, psize);
        }
        
        layerPointCanvas.repaint();
        
        if (debug) {
        	System.out.println("-- SusbspaceStreamPanel.drawPoint() --");
        	System.out.println("panel_width = " + getWidth() +
        					   ", panel_height = " + getHeight() + 
        					   ", size = " + size +
        					   ", x = " + x +
        					   ", y = " + y +
        					   ", psize = " + psize +
        					   ", poffset = " + poffset);
        }
    }


    /**
     * Draw clusterings in 'layer' onto canvas of 'imageGraphics'.
     * When points & clustering should be on screen at the same time, pointCanvas will hide clustering layer.
     * To solve this, use this method to draw the clusterings onto the pointCanvas again.
     * 
     * @param layer
     * @param imageGraphics
     */
    private void drawClusteringsOnCanvas(JPanel layer, Graphics2D imageGraphics) {
        for (Component comp : layer.getComponents()) {
            if (comp instanceof SubspaceClusterPanel) {
                SubspaceClusterPanel cp = (SubspaceClusterPanel) comp;
                cp.drawOnCanvas(imageGraphics);
            } else if (comp instanceof SubspacePointPanel) {
            	SubspacePointPanel pp = (SubspacePointPanel) comp;
            	pp.drawOnCanvas(imageGraphics);
            }
        }
    }


    public void applyDrawDecay(float factor) {
        RescaleOp brightenOp = new RescaleOp(1f, 150f/factor, null);
        pointCanvas = brightenOp.filter(pointCanvas, null);

        layerPointCanvas.setImage(pointCanvas);
        layerPointCanvas.repaint();
    }

    private void drawClustering(JPanel layer, Clustering clustering, Color color) {
        layer.removeAll();

        for (int c = 0; c < clustering.size(); c++) {
            SubspaceSphereCluster cluster = (SubspaceSphereCluster) clustering.get(c);

            SubspaceClusterPanel clusterpanel = new SubspaceClusterPanel(cluster, color, this);
            
            layer.add(clusterpanel);
            clusterpanel.updateLocation();
        }

        if (layer.isVisible() && pointsVisible) {
            Graphics2D imageGraphics = (Graphics2D) pointCanvas.createGraphics();
            imageGraphics.setColor(color);
            drawClusteringsOnCanvas(layer, imageGraphics);
            layerPointCanvas.repaint();
        }

        layer.repaint();
    }
    

    public void screenshot(String filename, boolean svg, boolean png){
    	if(layerPoints.getComponentCount()==0 && layerMacro.getComponentCount()==0 && layerMicro.getComponentCount()==0)
    		return;
    	
        BufferedImage image = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
        if(png){
            synchronized(getTreeLock()){
                Graphics g = image.getGraphics();
                paintAll(g);
                try {
                    ImageIO.write(image, "png", new File(filename+".png"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(svg){
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename+".svg")));
                int width = 500;
                out.write("<?xml version=\"1.0\"?>\n");
                out.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
                out.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\""+width+"\" height=\""+width+"\">\n");

                if(layerMicro.isVisible()){
                    for(Component comp :layerMicro.getComponents()){
                        if(comp instanceof ClusterPanel)
                            out.write(((ClusterPanel)comp).getSVGString(width));
                    }
                }

                if(layerMacro.isVisible()){
                    for(Component comp :layerMacro.getComponents()){
                        if(comp instanceof ClusterPanel)
                            out.write(((ClusterPanel)comp).getSVGString(width));
                    }
                }

                if(layerGroundTruth.isVisible()){
                    for(Component comp :layerGroundTruth.getComponents()){
                        if(comp instanceof ClusterPanel)
                            out.write(((ClusterPanel)comp).getSVGString(width));
                    }
                }

                if(layerPoints.isVisible()){
                    for(Component comp :layerPoints.getComponents()){
                        if(comp instanceof PointPanel){
                            PointPanel pp = (PointPanel) comp;
                            out.write(pp.getSVGString(width));
                        }
                    }
                }
                
                out.write("</svg>");
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(SubspaceStreamPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public SubspaceClusterPanel getHighlightedClusterPanel(){
        return highlighted_cluster;
    }

    public void setHighlightedClusterPanel(SubspaceClusterPanel clusterpanel){
        highlighted_cluster = clusterpanel;
        repaint();
    }

    public void setZoom(int x, int y, int zoom_delta, JScrollPane scrollPane){
        
        if(zoom ==1){
            width_org = getWidth();
            height_org = getHeight();
        }
        zoom+=zoom_delta;
        
        if(zoom<1) zoom = 1;
        else{
            int size = (int)(Math.min(width_org, height_org)*zoom_factor*zoom);

            setSize(new Dimension(size*zoom, size*zoom));
            setPreferredSize(new Dimension(size*zoom, size*zoom));

            scrollPane.getViewport().setViewPosition(new Point((int)(x*zoom_factor*zoom+x),(int)( y*zoom_factor*zoom+y)));
        }
    }

    public int getActiveXDim() {
        return activeXDim;
    }

    public void setActiveXDim(int activeXDim) {
        this.activeXDim = activeXDim;
    }

    public int getActiveYDim() {
        return activeYDim;
    }

    public void setActiveYDim(int activeYDim) {
        this.activeYDim = activeYDim;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(255, 255, 255));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        if(highlighted_cluster!=null){
            highlighted_cluster.highlight(false);
            highlighted_cluster=null;
        }
    }//GEN-LAST:event_formMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    
    
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        //System.out.println(e.getComponent().getClass().getName() + " --- Resized ");

        int size = Math.min(getWidth(), getHeight());
        layerMicro.setSize(new Dimension(size, size));
        layerMacro.setSize(new Dimension(size, size));
        layerGroundTruth.setSize(new Dimension(size, size));
        layerPoints.setSize(new Dimension(size, size));

        pointCanvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        layerPointCanvas.setSize(new Dimension(size,size));
        layerPointCanvas.setImage(pointCanvas);

        Graphics2D pointGraphics = (Graphics2D) pointCanvas.getGraphics();
        pointGraphics.setColor(Color.white);
        pointGraphics.fillRect(0, 0, getWidth(), getHeight());
        pointGraphics.dispose();
    }

    public void componentShown(ComponentEvent e) {
    }


}
