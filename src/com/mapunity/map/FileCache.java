package com.mapunity.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Image;

import com.mapunity.tracker.controller.Controller;
import com.mapunity.tracker.view.Logger;

/**
 * Caches tiles to the filesystem. A byproduct of this is that tiles can be
 * downloaded via an external program on a pc and transfered across by loading
 * them onto a memory card
 * 
 * @author gareth
 * 
 */
public class FileCache implements TileCache, Runnable {

    private FileConnection Conn = null;
    private DataOutputStream streamOut = null;
    private DataInputStream streamIn = null;
    private Vector fileProcessQueue = new Vector();
    // private PrintStream streamPrint = null;
    private Thread cacheThread = null;
    private static final int THREADDELAY = 200;//

    private static final String cacheName = "BTISCache";
    private String fullPath = "";
    private String exportFolder = "";

    // Default scope so it can be seen by the RMSCache
    Hashtable availableTileList = new Hashtable();

    public FileCache() {
        Logger.debug("FILE: FileCache ");
        exportFolder = Controller.getController().getSettings()
                .getExportFolder();


        fullPath = "file:///" + exportFolder + cacheName;
        Thread initThread = new Thread() {
            public void run() {
                initializeCache();
            }
        };
        //initThread.setPriority(Thread.MIN_PRIORITY);
        initThread.start();
        try {

            initThread.join();
        } catch (InterruptedException e1) {
            Logger.error("File: Error" + e1.getMessage());
            e1.printStackTrace();
        }
        cacheThread = new Thread(this);
        //cacheThread.setPriority(Thread.MIN_PRIORITY);
        cacheThread.start();
    }

    /**
     * Try to find a cache dir and if found, create a list of the files within
     * it. The files will be loaded only when they are requested
     */
    public void initializeCache() {
        Logger.debug("Initializing FileCache");
        

        try {
            Conn = (FileConnection) Connector.open(fullPath);
            if (Conn != null && !Conn.exists()) {
                // The file doesn't exist, we are done initializing
                Logger.debug("File: file does not exist");
                Conn.create();
            } else {
                streamIn = Conn.openDataInputStream();

                // streamOut = Conn.openDataOutputStream();
                Logger.debug("streamIn is " + streamIn + ", streamOut is "
                        + streamOut);
                Logger.debug("Conn.availableSize()=" + Conn.availableSize());
                boolean reading = true;
                while (reading) {
                    // There's no way of detecting the end of the stream
                    // short of getting an IOexception
                    try {

                        Tile t = Tile.getTile(streamIn);

                        Logger.debug("t is " + t.cacheKey + ", offset is "
                                + t.offset);
                        if (t != null) {
                            availableTileList.put(t.cacheKey,
                                    new Long(t.offset));
                        }
                    } catch (Exception ioe) {
                        reading = false;
                    }

                }
                Logger.debug("FILE: read " + availableTileList.size()
                        + " tiles");


                streamIn.close();

                streamIn = null;
            }

        } catch (IOException e) {
            Logger.error("File: IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    public long checkCacheOffset(String name) {
        long offset = -1;
        if (availableTileList.containsKey(name)) {
            offset = ((Long) availableTileList.get(name)).longValue();
        }
        return offset;
    }

    /**
     * Take a Vector of tiles and attempt to serialize them all to the
     * filesystem
     * 
     * @param tiles
     *                The vector of tiles to serialize
     * @return true if serialization was successful
     */
    public boolean writeToFileCache(Vector tiles) {
        boolean result = false;
        String fullPath = "";
        String exportFolder = Controller.getController().getSettings()
                .getExportFolder();
        fullPath = "file:///" + exportFolder + cacheName;
        Logger.debug("tiles " + tiles.size());
        try {
            // ------------------------------------------------------------------
            // Create a FileConnection and if this is a new stream create the
            // file
            // ------------------------------------------------------------------

            // Logger.debug("FILE: path is " + fullPath);
            
            //Conn will be created once, in the initCache method
            // if Conn is subsequently lost, too bad, we won't try to recreate it
            
            //if (Conn == null) {
            //    Conn = (FileConnection) Connector.open(fullPath);
           // }
            try {
                // Create file
                if (Conn != null && !Conn.exists()) {
                    Conn.create();
                } else {
                    // Logger.debug("File: file already exists, skipping: "
                    // + fullPath);
                }

            } catch (IOException ex) {
                Logger.error("writeAllToFileCache: Unable to open file : "
                        + fullPath + ", Full details : " + ex.toString());
            }

            if (Conn != null && streamOut == null) {
                // open the steam at the end so we can append to the file

                OutputStream x = Conn.openOutputStream(Conn.fileSize());

                streamOut = new DataOutputStream(x);


            } else {
                // Logger.debug("streamOut is not null");
            }

            if (streamOut != null) {

                boolean firstTile = true;
                while (fileProcessQueue.size() > 0) {

                    Tile t = (Tile) fileProcessQueue.firstElement();
                    // buffer=t.getImageByteArray();
                    fileProcessQueue.removeElementAt(0);

                   //Only serialize tile we know are not already serialized...
                  if(!checkCache(t.cacheKey)){
                    //The first tile will not have the correct offset if the file is
                    //not empty, so we should give it the offset
                    if (firstTile) {
                        t.serialize(streamOut, Conn.fileSize());
                        firstTile=false;
                    } else {
                        t.serialize(streamOut);
                    }
                    // streamOut.write(buffer, 0, buffer.length);

                    streamOut.flush();

                    // Specifically keep the file OPEN, this should prevent too
                    // many
                    // Permission requests
                    // streamOut.close();
                    // outConn.close();
                    result = true;
                    availableTileList.put(t.cacheKey,
                            new Long(Tile.totalOffset));
                    Logger.debug("availableTileList size="
                            + availableTileList.size());
                  }else{
                      Logger.debug("Not Writing tile, already serialized:" +t.cacheKey);
                  }
                }
                streamOut.close();
            } else {
                Logger.debug("File: output stream is null");
            }
            
            
            streamOut = null;

        } catch (IOException e) {
            Logger.debug("FILE: error:" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public Image getImage(String name) {

        Image out = null;

        try {
            out = getTile(name).getImage();
        } catch (Exception e) {
            Logger.error("FileCache:" + e.getMessage());
            e.printStackTrace();
        }

        return out;

    }

    public Tile getTile(String name) {
        Tile t = null;
        boolean reading = true;
        if (checkCache(name)) {

            if (Conn != null) {
                try {
                    if (streamIn == null) {
                        InputStream x = Conn.openInputStream();
                        Logger.debug("Skipping " + checkCacheOffset(name)
                                + " bytes");
                        x.skip(checkCacheOffset(name));

                        streamIn = new DataInputStream(x);
                    }
                    if (streamIn != null) {

                       // int counter = 0;
                        while (reading) {
                            try {
                                // Assuming that a concatenated bunch of tiles
                                // can
                                // be deserialized
                                // one at a time this way
                                t = Tile.getTile(streamIn);
                                if (t != null && t.cacheKey != null
                                        && t.cacheKey.equals(name)) {
                                    // Found the right tile
                                  //  counter++;
                                  //  Logger.debug("Found tile after " + counter
                                    //        + " iterations");
                                    break;
                                }
                                //counter++;
                            } catch (IOException e) {
                                Logger.debug("Didn't find the tile...");
                                reading = false;
                                e.printStackTrace();
                            } catch (Exception e) {
                                Logger.debug("Didn't find the tile...");
                                reading = false;
                                e.printStackTrace();
                            }
                        }
                        streamIn.close();
                        streamIn = null;
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (NullPointerException npe) {
                    Logger.debug("Caught NPE: name is " + name);
                }

            }
        }
        return t;
    }

    public boolean checkCache(String name) {
        if (availableTileList.containsKey(name)) {
            Logger.debug("Found tile in filecache: " + name);
            return true;
        } else {
          //  Logger.debug("Didn't find tile in filecache: " + name);
            return false;
        }
    }


    private void addToQueue(Tile tile) {
        Logger.debug("FILE:Adding Tile to File queue");
        synchronized (fileProcessQueue) {
            if (!fileProcessQueue.contains(tile)) {
                fileProcessQueue.addElement(tile);
            }
        }
        Logger.debug("FILE: FILE queue size now " + fileProcessQueue.size());
    }


    /**
     * This version will write the whole list out as one file in order to reduce
     * the amount of times permission needs to be sought.
     */
    public void run() {
        Thread thisThread = Thread.currentThread();


        try {
            // Logger.debug("FILE:Initialized ok, now sleeping for 1sec");

            Thread.sleep(THREADDELAY);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (cacheThread == thisThread) {

            try {

                Thread.sleep(THREADDELAY);
            } catch (InterruptedException e) {
                // Logger.debug("FileCache:Thread was interrupted");


            }
            synchronized (fileProcessQueue) {

                try {
                    if (fileProcessQueue.size() > 0) {

                        Logger.debug("FILE: FILE queue size is:"
                                + fileProcessQueue.size());


                        try {
                            // Logger.debug("FILE: " + cacheName);

                            writeToFileCache(fileProcessQueue);


                        } catch (Exception e) {
                            Logger
                                    .error("FILE: Exception while writing tile to filesystem: "
                                            + e.getMessage());
                        }
                    } else {
                        // Logger.getLogger()
                        // .log("FILE: FILEProcessQueueEmpty, yielding "+
                        // fileProcessQueue.size(), Logger.DEBUG);
                        Thread.yield();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }


    }

    public void put(Tile tile) {
        addToQueue(tile);

    }

    /**
     * Checks that the cache contains only valid tile information. One of the
     * issues that can affect the file cache is being interrupted while writing
     * a tile out. This will often result in a foreshortened byte array. This
     * can be detected as the size of the array is written out immediately
     * before the byte array. Other things to check for are that the xyz ints
     * are all within the expected range (0-2^18ish) and the Strings are not
     * null. TODO: Implement this
     * 
     * @return
     */
    private boolean verifyCacheIntegrity() {
        return true;
    }

}
