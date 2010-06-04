package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class AxisVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Axes";

  /**
   * Initializes this Visualizer.
   * 
   * @param context visualization context
   */
  public void init(VisualizerContext<? extends NV> context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new AxisVisualization(context, svgp, proj, width, height);
  }

  /**
   * Axis visualization.
   * 
   * @author Erich Schubert
   */
  protected class AxisVisualization extends Projection2DVisualization<NV> {
    /**
     * Container element.
     */
    private Element container;

    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public AxisVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height) {
      super(context, svgp, proj, width, height);
      double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
      this.container = super.setupCanvas(svgp, proj, margin, width, height);
      this.layer = new VisualizationLayer(Visualizer.LEVEL_BACKGROUND, this.container);
      redraw();
    }

    @Override
    protected void redraw() {
      // Implementation note: replacing the container element is faster than
      // removing all markers and adding new ones - i.e. a "bluk" operation
      // instead of incremental changes
      Element oldcontainer = null;
      if(container.hasChildNodes()) {
        oldcontainer = container;
        container = (Element) container.cloneNode(false);
      }

      int dim = context.getDatabase().dimensionality();

      // origin
      Vector orig = proj.projectScaledToRender(new Vector(dim));
      // diagonal point opposite to origin
      Vector diag = new Vector(dim);
      for(int d2 = 0; d2 < dim; d2++) {
        diag.set(d2, 1);
      }
      diag = proj.projectScaledToRender(diag);
      // compute angle to diagonal line, used for axis labeling.
      double diaga = Math.atan2(diag.get(1) - orig.get(1), diag.get(0) - orig.get(0));

      double alfontsize = 1.2 * context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL);
      CSSClass alcls = new CSSClass(svgp, "unmanaged");
      alcls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(alfontsize));
      alcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
      alcls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));

      // draw axes
      for(int d = 1; d <= dim; d++) {
        Vector v = new Vector(dim);
        v.set(d - 1, 1);
        // projected endpoint of axis
        Vector ax = proj.projectScaledToRender(v);
        boolean righthand = false;
        double axa = Math.atan2(ax.get(1) - orig.get(1), ax.get(0) - orig.get(0));
        if(axa > diaga || (diaga > 0 && axa > diaga + Math.PI)) {
          righthand = true;
        }
        // System.err.println(ax.get(0) + " "+ ax.get(1)+
        // " "+(axa*180/Math.PI)+" "+(diaga*180/Math.PI));
        if(ax.get(0) != orig.get(0) || ax.get(1) != orig.get(1)) {
          try {
            SVGSimpleLinearAxis.drawAxis(svgp, container, proj.getScale(d), orig.get(0), orig.get(1), ax.get(0), ax.get(1), true, righthand, context.getStyleLibrary());
            // TODO: move axis labeling into drawAxis function.
            double offx = (righthand ? 1 : -1) * 0.02 * VisualizationProjection.SCALE;
            double offy = (righthand ? 1 : -1) * 0.02 * VisualizationProjection.SCALE;
            Element label = svgp.svgText(ax.get(0) + offx, ax.get(1) + offy, "Dim. " + SVGUtil.fmt(d));
            SVGUtil.setAtt(label, SVGConstants.SVG_STYLE_ATTRIBUTE, alcls.inlineCSS());
            SVGUtil.setAtt(label, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, righthand ? SVGConstants.SVG_START_VALUE : SVGConstants.SVG_END_VALUE);
            container.appendChild(label);
          }
          catch(CSSNamingConflict e) {
            throw new RuntimeException("Conflict in CSS naming for axes.", e);
          }
        }
      }
      if(oldcontainer != null && oldcontainer.getParentNode() != null) {
        oldcontainer.getParentNode().replaceChild(container, oldcontainer);
      }
    }
  }
}