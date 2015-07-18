package org.tracetool.hackconnectivityservice;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import android.content.SharedPreferences;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import com.android.internal.util.XmlUtils;


/**
 * This class is basically the same as SharedPreferencesImpl from AOSP, but 
 * read-only and without listeners support. Instead, it is made to be
 * compatible with all ROMs.
 */
public final class XSharedPreferences2 implements SharedPreferences {
	private static final String TAG = "TT2";
	private final File mFile;
	private Map<String, Object> mMap;
	private boolean mLoaded = false;
    private long mLastModified;
    private long mFileSize;
    
	public XSharedPreferences2(File prefFile) {
		mFile = prefFile;
		startLoadFromDisk();
    }
	
	public XSharedPreferences2(String packageName) {
		this(packageName, packageName + "_preferences");
   }
	
	public XSharedPreferences2(String packageName, String prefFileName) {
		mFile = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
		startLoadFromDisk();
   }
	
    private void startLoadFromDisk() 
    {
       //Log.i(TAG, "(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " +       
       //"startLoadFromDisk : " +  mFile.getPath());
       synchronized (XSharedPreferences2.this) {
            mLoaded = false;
       }
       new Thread("XSharedPreferences-load") {
           @Override
           public void run() {
               synchronized (XSharedPreferences2.this) {
                   loadFromDiskLocked();
               }
           }
       }.start();
    }
    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadFromDiskLocked() {
        if (mLoaded) {
            return;
        }
        
        Map map = null;
        long lastModified = 0;
        long fileSize = 0;
        if (mFile.canRead()) {
           lastModified = mFile.lastModified();
           fileSize = mFile.length();
           BufferedInputStream str = null;
           try {
              str = new BufferedInputStream(
                    new FileInputStream(mFile), 
                    16*1024);
              map = XmlUtils.readMapXml(str);
              str.close();
           } catch (XmlPullParserException e) {
              Log.w(TAG,"(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " + "getSharedPreferences", e);
           } catch (IOException e) {
              Log.w(TAG,"(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " + "getSharedPreferences", e);
           } finally {
              if (str != null) {
                 try {
                    str.close();
                 } catch (RuntimeException rethrown) {
                    throw rethrown;
                 } catch (Exception ignored) {
                 }
              }
           }
        } else {
           //Log.w(TAG,"(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " + "loadFromDiskLocked cannot read config file");
           
        }
        mLoaded = true;
        if (map != null) {
            mMap = map;
            mLastModified = lastModified;
            mFileSize = fileSize;
        } else {
            mMap = new HashMap<String, Object>();
        }
        notifyAll();
    }
	
	/**
	 * Reload the settings from file if they have changed.
	 */
	public void reload() {
        synchronized (XSharedPreferences2.this) {
        	if (hasFileChanged())
        		startLoadFromDisk();
        }
	}
	
	private boolean hasFileChanged() {
        if (!mFile.canRead()) {
            return true;
        }
        long lastModified = mFile.lastModified();
        long fileSize = mFile.length();
        synchronized (XSharedPreferences2.this) {
            return mLastModified != lastModified || mFileSize != fileSize;
        }
    }
    
    private void awaitLoadedLocked() {
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException unused) {
            }
        }
    }
	
    @Override
    public Map<String, ?> getAll() {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            return new HashMap<String, Object>(mMap);
        }
    }

    @Override
    public String getString(String key, String defValue) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            Integer v = (Integer)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    
    @Override
    public long getLong(String key, long defValue) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            Long v = (Long)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            Float v = (Float)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            Boolean v = (Boolean)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public boolean contains(String key) {
        synchronized (XSharedPreferences2.this) {
            awaitLoadedLocked();
            return mMap.containsKey(key);
        }
    }

	@Override
	public Editor edit() {
		//throw new UnsupportedOperationException("read-only implementation");

      synchronized (XSharedPreferences2.this) {
         awaitLoadedLocked();
     }

     return new EditorImpl();

	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		throw new UnsupportedOperationException("listeners are not supported in this implementation");
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		throw new UnsupportedOperationException("listeners are not supported in this implementation");
	}

   public final class EditorImpl implements Editor {
      public boolean writeToDiskResult = false;
      
      public Editor putString(String key, String value) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, value);
              return this;
          }
      }
      public Editor putStringSet(String key, Set<String> values) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, values);
              return this;
          }
      }
      public Editor putInt(String key, int value) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, value);
              return this;
          }
      }
      public Editor putLong(String key, long value) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, value);
              return this;
          }
      }
      public Editor putFloat(String key, float value) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, value);
              return this;
          }
      }
      public Editor putBoolean(String key, boolean value) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, value);
              return this;
          }
      }

      public Editor remove(String key) {
          synchronized (XSharedPreferences2.this) {
              mMap.put(key, this);
              return this;
          }
      }

      public Editor clear() {
          synchronized (XSharedPreferences2.this) {
              //mClear = true;
              return this;
          }
      }

      public void apply() 
      {
         commit() ;
      }

      public boolean commit() {
         final Runnable writeToDiskRunnable = new Runnable() {
            public void run() {
               synchronized (XSharedPreferences2.this) {
                  writeToFile();
               }
            }
         };
         writeToDiskRunnable.run();
         return writeToDiskResult ;
      }


      private void writeToFile() {
         //if (mFile.exists()) 
         //   mFile.delete();
         
         try {
             //mFile.createNewFile();
             FileOutputStream str = createFileOutputStream(mFile);
             if (str == null) {
                 writeToDiskResult = false;
                 return;
             }
             XmlUtils.writeMapXml(mMap, str);
             FileUtils.sync(str);
             str.close();
             mFile.setReadable(true, false) ;
             mFile.setWritable(true, false) ;             

             writeToDiskResult = true;
             return;
         } catch (XmlPullParserException e) {
             Log.w(TAG, "(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " +"writeToFile: Got exception:", e);
         } catch (IOException e) {
             Log.w(TAG, "(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " +"writeToFile: Got exception:", e);
         }

         writeToDiskResult = false;
     }

      private FileOutputStream createFileOutputStream(File file) {
         FileOutputStream str = null;
         try {
             // FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_WORLD_WRITEABLE);   // CONTEXT !
            
             str = new FileOutputStream(file);
         } catch (FileNotFoundException e) {
             File parent = file.getParentFile();
             if (!parent.mkdir()) {
                 Log.e(TAG,"(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " + 
                           "Couldn't create directory for SharedPreferences file " + file);
                 return null;
             }
             FileUtils.setPermissions(
                 parent.getPath(),
                 FileUtils.S_IRWXU|FileUtils.S_IRWXG|FileUtils.S_IXOTH,
                 -1, -1);
             try {
                 str = new FileOutputStream(file);
             } catch (FileNotFoundException e2) {
                 Log.e(TAG, "(" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " +
                            "Couldn't create SharedPreferences file " + file, e2);
             }
         }
         return str;
     }

  }

}
