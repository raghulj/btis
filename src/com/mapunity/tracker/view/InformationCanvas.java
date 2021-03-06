/*
 * InformationCanvas.java
 *
 * Copyright (C) 2005-2007 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package com.mapunity.tracker.view;

import com.mapunity.gps.GpsPosition;
import com.mapunity.tracker.model.LengthFormatter;
import com.mapunity.tracker.model.SpeedFormatter;
import com.mapunity.tracker.model.Track;
import com.mapunity.tracker.model.UnitConverter;
import com.mapunity.util.DateTimeUtil;
import com.mapunity.util.StringUtil;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Information canvas is used to display textual information about the current
 * location.
 *
 * @author Tommi Laukkanen
 */
public class InformationCanvas extends BaseCanvas{
    
    private int lineRow;
    private int firstRow;
    private int totalTextHeight;
    private int displayHeight;

    private final static Font BIG_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private final static Font SMALL_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private final static int VALUE_COL = BIG_FONT.stringWidth("LAT:_:");
    private final static int BIG_VALUE_COL = BIG_FONT.stringWidth("LAT ALT:_:");
    
    /** Creates a new instance of InformationCanvas */
    public InformationCanvas() {
        super();
        firstRow = 0;
        totalTextHeight = 0;
        displayHeight = this.getHeight();
    }    
    
    /** 
     * Paint information canvas
     * @param g Graphics
     */
    protected void paint(Graphics g) {
        
        displayHeight = getHeight();
        totalTextHeight = 0;
        
        g.setColor(Theme.getColor(Theme.TYPE_BACKGROUND));
        g.fillRect(0,0,getWidth(),getHeight());
        
        // Draw the title
        g.setColor(Theme.getColor(Theme.TYPE_TITLE));
        g.setFont(titleFont);
        if(firstRow==0) {
            g.drawString("Information", getWidth()/2,1,Graphics.TOP|Graphics.HCENTER);
        }
        
        final int titleHeight = 2 + titleFont.getHeight();
        Logger.debug("InformationCanvas getPosition called");
        GpsPosition position = controller.getPosition();
        
        g.setFont(BIG_FONT);
        
        int charHeight = BIG_FONT.getHeight();
        String lat = "";
        String lon = "";
        String spd = "";
        String hea = "";
        String alt = "";
        String dst = "";
        String durationTime = "";
        String maximumSpeed = "";
        String averageSpeed = "";
        
        Track currentTrack = controller.getTrack();
        LengthFormatter lengthFormatter = new LengthFormatter( controller.getSettings() );
        if(position!=null) {
            lat = StringUtil.valueOf(position.latitude, 4);
            lon = StringUtil.valueOf(position.longitude, 4);
            
            SpeedFormatter formatter = new SpeedFormatter( controller.getSettings() );
            spd = formatter.getSpeedString(position.speed);
            
            hea = position.getHeadingString();
            
            alt = lengthFormatter.getLengthString(position.altitude, false);

            if(currentTrack!=null) {
                dst = lengthFormatter.getLengthString(currentTrack.getDistance(), true); 
                if(currentTrack.getStartPosition()!=null &&
                        currentTrack.getEndPosition()!=null) {
                    durationTime = DateTimeUtil.getTimeInterval(
                        currentTrack.getStartPosition().date, 
                        currentTrack.getEndPosition().date);   
                }
                if(currentTrack.getMaxSpeedPosition()!=null) {
                    maximumSpeed = UnitConverter.getSpeedString(
                        currentTrack.getMaxSpeedPosition().speed, 
                        controller.getSettings().getUnitsAsKilometers(),
                        true);
                }
                if(currentTrack.getAverageSpeed()!= 0) {
                    averageSpeed = UnitConverter.getSpeedString(
                    currentTrack.getAverageSpeed(), 
                    controller.getSettings().getUnitsAsKilometers(),
                    true);
                }
            }
        }
        int infoPos = BIG_FONT.stringWidth("LAT:_:");
        lineRow = titleHeight - firstRow;
        totalTextHeight = titleHeight;
        
        drawNextHeader(g, "Position");        
        drawNextString(g, "LAT", lat);
        drawNextString(g, "LON", lon);
        drawNextString(g, "ALT", alt);
        drawNextString(g, "HEA", hea);
        
        drawNextHeader(g, "Speed");        
        drawNextString(g, "SPD", spd);
        drawNextString(g, "AVG", averageSpeed);
        drawNextString(g, "MAX", maximumSpeed);

        drawNextHeader(g, "Trail");
        drawNextString(g, "DST", dst);
        drawNextString(g, "DUR", durationTime);
        if(currentTrack!=null) {
            if(currentTrack.getMinAltitudePosition()!=null) {
                double minAltitude = currentTrack.getMinAltitudePosition().altitude;
                String minAltString = lengthFormatter.getLengthString(minAltitude, false);
                double maxAltitude = currentTrack.getMaxAltitudePosition().altitude;
                String maxAltString = lengthFormatter.getLengthString(maxAltitude, false);
                String trailAltitude = minAltString + " - " + maxAltString;
                drawNextString(g, "ALT", trailAltitude);
            }
        }
      
    }
    
    private void drawNextString(Graphics g, String name, String value) {
        if(lineRow<-BIG_FONT.getHeight()) {
            return;
        }
        g.setFont(BIG_FONT);
        g.setColor( Theme.getColor(Theme.TYPE_TEXT) );
        g.drawString(name, 1, lineRow, Graphics.TOP|Graphics.LEFT);
        g.setColor( Theme.getColor(Theme.TYPE_TEXTVALUE) );
        int column = (name.length()>4 ? BIG_VALUE_COL : VALUE_COL);
        g.drawString(value, column, lineRow, Graphics.TOP|Graphics.LEFT);
        lineRow += BIG_FONT.getHeight();
        totalTextHeight += BIG_FONT.getHeight();
    }
    
    private void drawNextHeader(Graphics g, String header) {
        if(lineRow<-SMALL_FONT.getHeight()) {
            return;
        }
        g.setFont(SMALL_FONT);
        g.setColor( Theme.getColor(Theme.TYPE_SUBTITLE) );
        g.drawString(header, getWidth()/2, lineRow, Graphics.TOP|Graphics.HCENTER);
        lineRow += SMALL_FONT.getHeight();
        totalTextHeight += SMALL_FONT.getHeight();
    }
    
    /** Key pressed handler */
    protected void keyPressed(int keyCode) {
        super.keyPressed(keyCode); 
        handleKeys(keyCode);
    }

    /** 
     * Key pressed many times
     * @param keyCode 
     */
    protected void keyRepeated(int keyCode) {
        super.keyRepeated(keyCode);
        handleKeys(keyCode);
    }

    /** Handle up/down keys */
    private void handleKeys(int keyCode) {
        int gameKey = getGameAction(keyCode);
        /** Handle up/down presses so that informations are scrolled */
        if(gameKey==Canvas.UP) {
            firstRow -= BIG_FONT.getHeight();
            if(firstRow<0) {
                firstRow = 0;
            }
        }
        /** Handle up/down presses so that informations are scrolled */
        if(gameKey==Canvas.DOWN) {
            if(firstRow < totalTextHeight-displayHeight) {
                firstRow += BIG_FONT.getHeight();
            }
        }        
    }
    
}
