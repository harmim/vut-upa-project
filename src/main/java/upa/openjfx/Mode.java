package upa.openjfx;

public class Mode {
  public enum workingMode {
    Rect,
    Circle,
    Collection,
    Polyline,
    Multipoint,
    Point,
    Delete,
    Move,
    Resize,
    None,
    addPointToMP,
    addPointToPL,
  };

  public enum transactionMode {
    addPointToMP,
    addPointToPL,
    addCircleToCollection,
    removePointFromMP,
    removePointFromPL,
    removeCircleFromCollection,
    None,
  }
}
