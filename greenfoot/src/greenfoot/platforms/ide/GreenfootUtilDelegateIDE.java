/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.platforms.ide;

import greenfoot.GreenfootImage;
import greenfoot.PlayerData;
import greenfoot.GreenfootStorageVisitor;
import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootStorageException;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import bluej.Config;
import bluej.runtime.ExecServer;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * GreenfootUtilDelegate implementation for the Greenfoot IDE.
 */
public class GreenfootUtilDelegateIDE implements GreenfootUtilDelegate
{
    private static GreenfootUtilDelegateIDE instance;
    
    private String curUserName = "Player";
    
    /** A soft reference to a cached image */
    private class CachedImageRef extends SoftReference<GreenfootImage>
    {
        String imgName;
        
        public CachedImageRef(String imgName, GreenfootImage image, ReferenceQueue<GreenfootImage> queue)
        {
            super(image, queue);
            this.imgName = imgName;
        }
    }
    
    private Map<String,CachedImageRef> imageCache = new HashMap<String,CachedImageRef>();
    private ReferenceQueue<GreenfootImage> imgCacheRefQueue = new ReferenceQueue<GreenfootImage>();
    
    static {
        instance = new GreenfootUtilDelegateIDE();
    }
    
    /**
     * Get the GreenfootUtilDelegateIDE instance.
     */
    public static GreenfootUtilDelegateIDE getInstance()
    {
        return instance;
    }
    
    private GreenfootUtilDelegateIDE()
    {
        // Nothing to do.
    }
    
    /**
     * Creates the skeleton for a new class
     */
    @Override
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
            throws IOException
    {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if(superClassName != null) {
            translations.put("EXTENDSANDSUPERCLASSNAME", "extends " + superClassName);
        } else {
            translations.put("EXTENDSANDSUPERCLASSNAME", "");
        }
        String baseName = "greenfoot/templates/" +  templateFileName;
        File template = Config.getLanguageFile(baseName);
        
        if(!template.canRead()) {
            template = Config.getDefaultLanguageFile(baseName);
        }
        BlueJFileReader.translateFile(template, file, translations, Charset.forName("UTF-8"), Charset.defaultCharset());
    }
    
    @Override
    public URL getResource(String path) 
    {
        return ExecServer.getCurrentClassLoader().getResource(path);
    }
    
    @Override
    public Iterable<String> getSoundFiles()
    {
        ArrayList<String> files = new ArrayList<String>();
        try
        {
            URL url = getResource("sounds");
            if (url != null && "file".equals(url.getProtocol()))
            {
                for (String file : new File(url.toURI()).list())
                {
                    files.add(file);
                }
            }
        }
        catch (URISyntaxException e)
        {
            Debug.reportError("Bad URI in getResources", e);
        }
        // May be a blank list if something went wrong:
        return files;
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    @Override
    public  String getGreenfootLogoPath()
    {        
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";        
    }

    @Override
    public boolean addCachedImage(String fileName, GreenfootImage image) 
    {
        synchronized (imageCache) {
            if (image != null) {
                CachedImageRef cr = new CachedImageRef(fileName, image, imgCacheRefQueue);
                imageCache.put(fileName, cr);
            }
            else {
                imageCache.put(fileName, null);
            }
        }
        return true;
    }

    @Override
    public GreenfootImage getCachedImage(String fileName)
    { 
        synchronized (imageCache) {
            flushImgCacheRefQueue();
            CachedImageRef sr = imageCache.get(fileName);
            if (sr != null) {
                return sr.get();
            }
            return null;
        }
    }

    @Override
    public void removeCachedImage(String fileName)
    {
        synchronized (imageCache) {
            CachedImageRef cr = imageCache.remove(fileName);
            if (cr != null) {
                cr.clear();
            }
        }
    }

    @Override
    public boolean isNullCachedImage(String fileName)
    {
        synchronized (imageCache) {
            return imageCache.containsKey(fileName) && imageCache.get(fileName) == null;
        }
    }

    /**
     * Clear the image cache.
     */
    public void clearImageCache()
    {
        synchronized (imageCache) {
            imageCache.clear();
            imgCacheRefQueue = new ReferenceQueue<GreenfootImage>();
        }
    }
    
    /**
     * Flush the image cache reference queue.
     * <p>
     * Because the images are cached via soft references, the references may be cleared, but the
     * key will still map to the (cleared) reference. Calling this method occasionally removes such
     * dead keys.
     */
    private void flushImgCacheRefQueue()
    {
        Reference<? extends GreenfootImage> ref = imgCacheRefQueue.poll();
        while (ref != null) {
            if (ref instanceof CachedImageRef) {
                CachedImageRef cr = (CachedImageRef) ref;
                imageCache.remove(cr.imgName);
            }
            ref = imgCacheRefQueue.poll();
        }
    }
    
    @Override
    public void displayMessage(Component parent, String messageText)
    {
        DialogManager.showText(parent, messageText);
    }

    @Override
    public boolean isStorageSupported()
    {
        return true;
    }
    
    public String getUserName()
    {
        return curUserName;
    }
    
    public void setUserName(String curUserName)
    {
        this.curUserName = curUserName;
    }

    @Override
    public PlayerData getCurrentUserData()
    {
        try
        {
            FileReader fr = new FileReader("storage.csv");
            CSVReader csv = new CSVReader(fr);
            
            for (String[] line : csv.readAll())
            {
                if (line.length > 1 && getUserName().equals(line[0]))
                {
                    return makeStorage(line);
                }
            }
            
            // Couldn't find them anywhere, return blank:
            return GreenfootStorageVisitor.allocate(getUserName());
        }
        catch (FileNotFoundException e)
        {
            // No previous storage, return blank:
            return GreenfootStorageVisitor.allocate(getUserName());
        }
        catch (IOException e)
        {
            Debug.message("Error reading user data: " + e.getMessage());
            return null;
        }
    }

    private PlayerData makeStorage(String[] line)
    {
        PlayerData r = null;
        try
        {
            int column = 0; 
            r = GreenfootStorageVisitor.allocate(line[column++]);
            r.setScore(Integer.parseInt(line[column++]));
            for (int i = 0; i < PlayerData.NUM_INTS; i++)
            {
                r.setInt(i, Integer.parseInt(line[column++]));
            }
            
            for (int i = 0; i < PlayerData.NUM_STRINGS; i++)
            {
                r.setString(i, line[column++]);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            // If we run out of the line, just stop setting the values in storage
        }
        
        return r;
    }
    

    private String[] makeLine(String userName, PlayerData data)
    {
        String[] line = new String[1 + 1 + PlayerData.NUM_INTS + PlayerData.NUM_STRINGS];
        int column = 0;
        line[column++] = userName;
        line[column++] = Integer.toString(data.getScore());
        try
        {
            for (int i = 0; i < PlayerData.NUM_INTS; i++)
            {
                line[column++] = Integer.toString(data.getInt(i));
            }
            
            for (int i = 0; i < PlayerData.NUM_STRINGS; i++)
            {
                line[column++] = data.getString(i);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            // Can't happen
        }        
        return line;
    }

    @Override
    public boolean storeCurrentUserData(PlayerData data)
    {
        List<String[]> all;
        try
        {
            FileReader fr = new FileReader("storage.csv");
            CSVReader csv = new CSVReader(fr);
            all = csv.readAll();
            csv.close();
        }
        catch (FileNotFoundException e)
        {
            // No previous storage, make a new blank one:
            all = new ArrayList<String[]>();
        }
        catch (IOException e)
        {
            Debug.message("Error reading user data: " + e.getMessage());
            return false;
        }
        
        // First, remove any existing line:
        Iterator<String[]> lineIt = all.iterator();
        while (lineIt.hasNext())
        {
            String[] line = lineIt.next();
            if (line.length > 1 && getUserName().equals(line[0]))
            {
                lineIt.remove();
                break;
            }
        }
        
        // Then add a line on to the end:
        if (data != null)
        {
            // No line for that user, add a new one:
            String[] line = new String[1 + PlayerData.NUM_INTS + PlayerData.NUM_STRINGS];
            all.add(makeLine(getUserName(), data));
        }
        
        try
        {
            CSVWriter csvOut = new CSVWriter(new FileWriter("storage.csv"));
            csvOut.writeAll(all);
            csvOut.close();
            return true;
        }
        catch (IOException e)
        {
            Debug.message("Error storing user data: " + e.getMessage());
            return false;
        }
    }
    
    private ArrayList<PlayerData> getAllDataSorted()
    {
        try
        {
            ArrayList<PlayerData> ret = new ArrayList<PlayerData>();
            
            FileReader fr = new FileReader("storage.csv");
            CSVReader csv = new CSVReader(fr);
            
            for (String[] line : csv.readAll())
            {
                ret.add(makeStorage(line));
            }
            
            Collections.sort(ret, new Comparator<PlayerData>() {
                @Override
                public int compare(PlayerData o1, PlayerData o2)
                {
                    // Sort in reverse order:
                    return -(o1.getInt(0) - o2.getInt(0));
                }
            });
            
            return ret;
        }
        catch (FileNotFoundException e)
        {
            // No previous storage, return the blank list:
            return new ArrayList<PlayerData>();
        }
        catch (IOException e)
        {
            Debug.message("Error reading user data: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<PlayerData> getTopUserData(int limit)
    {
        ArrayList<PlayerData> ret = getAllDataSorted();
        if (ret == null)
            return null;
        else if (ret.size() <= limit)
            return ret;
        else
            return ret.subList(0, limit);
    }

    @Override
    public GreenfootImage getUserImage(String userName)
    {
        // GreenfootUtil will take care of making a dummy image:
        return null;
    }

    @Override
    public List<PlayerData> getNearbyUserData(int maxAmount)
    {
        ArrayList<PlayerData> all = getAllDataSorted();
        
        if (all == null)
            return null;
        
        int index = -1;
        
        for (int i = 0; i < all.size();i++)
        {
            if (curUserName != null && curUserName.equals(all.get(i).getUserName()))
            {
                index = i;
            }
        }
        
        if (index == -1 || maxAmount == 0)
            return new ArrayList<PlayerData>();
        
        int availableBefore = index;
        int availableAfter = all.size() - 1 - index;
        
        int desiredBefore = maxAmount / 2;
        int desiredAfter = Math.max(0, maxAmount - 1) / 2;
        
        // maxAmount | desiredBefore | desiredAfter | before+after+1
        // 1 | 0 | 0 | 1
        // 2 | 1 | 0 | 2
        // 3 | 1 | 1 | 3
        // 4 | 2 | 1 | 4
        // 5 | 2 | 2 | 5
        // 6 | 3 | 2 | 6
        // and so on...
        
        if (availableAfter + availableBefore + 1 <= maxAmount)
        {
            //Less overall that we want, use everything:
            return all;
        }
        else if (availableBefore <= desiredBefore)
        {
            // Not enough available before-hand, but must be enough in total:
            return all.subList(index - availableBefore, index - availableBefore + maxAmount + 1);
        }
        else if (availableAfter <= desiredAfter)
        {
            // Not enough available after, but must be enough in total:
            return all.subList(index + availableAfter - maxAmount, index + availableAfter + 1);
        }
        else
        {
            // Must have enough available before and after:
            return all.subList(index - desiredBefore, index + desiredAfter + 1);
        }
    }
    
    
}
