package com.mapunity.map;

/**
 * Concrete MapProvider instance to provide map data from the tiles@home project
 * @author gareth
 *
 */
public class OsmMapProvider extends AbstractMapProvider {
    
    private final int maxZoomLevel=17;
    private final int minZoomLevel=0;
    
    public OsmMapProvider(){
        storeName="osmmaps";
        //UrlFormat="http://tile.openstreetmap.org/X/X/X.png";
        UrlFormat = "http://tilesX.mapunity.in/tiles/mobile/tiles/tile_X_X_X.png";
        //128 tiles
       // UrlFormat = "http://127.0.0.X/tiles/oltiles/tile_X_X_X.png";
      // UrlFormat = "http://tilesX.mapunity.in/tiles/spinfotiles/tile_X_X_X.png";
       //UrlFormat = "http://tilesX.mapunity.in/tiles/oltiles/tile_X_X_X.png";
        cacheDir=""; //Not used any more, now we have the FileCache
        displayString="Draw OSM Maps";
    }

       public int validateZoomLevel(int z){
        if(z>maxZoomLevel)
            z=maxZoomLevel;
        if(z<minZoomLevel)
            z=minZoomLevel;
        
        return z;
    }




}
