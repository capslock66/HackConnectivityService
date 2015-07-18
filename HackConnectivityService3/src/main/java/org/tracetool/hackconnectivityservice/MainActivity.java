package org.tracetool.hackconnectivityservice;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;

// from full_framework_15.jar
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.os.INetworkManagementService;

import de.robv.android.xposed.XposedHelpers;

public class MainActivity extends Activity 
{
   private Button   butRefresh;
   private Button   butScan;
   
   private Button   butActivateWifi;
   private Button   butActivateMobile;
   private Button   butDesactivateHack;
   private Button   butActivateTraces;
   private Button   butDesactivateTraces;
   private TextView hackStateValue ;
   
   private Handler mHandler = new Handler();
   private Runnable mUpdateTimeTask ;
   
   private Config config ;
   private int hackMode ;
   private int traceLevel ;


   @Override
   @SuppressLint("NewApi")
   protected void onCreate(Bundle savedInstanceState) 
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      this.hackStateValue       = (TextView) this.findViewById(R.id.hackStateValue); 
      this.butRefresh           = (Button)   this.findViewById(R.id.butRefresh);
      this.butActivateWifi      = (Button)   this.findViewById(R.id.butActivateWifi); 
      this.butActivateMobile    = (Button)   this.findViewById(R.id.butActivateMobile);       
      this.butDesactivateHack   = (Button)   this.findViewById(R.id.butDesactivateHack);   
      this.butActivateTraces    = (Button)   this.findViewById(R.id.butActivateTraces); 
      this.butDesactivateTraces = (Button)   this.findViewById(R.id.butDesactivateTraces); 
      
      config = new Config() ;  // all methods of this class are hoocked by the hack, if well installed !!!
      
      hackMode   = config.getHackMode() ;   // Controller.getFakeConnectionActivated() ;
      traceLevel = config.getTraceLevel() ; // Controller.getTraceLevel() ;
      showHackState(hackMode,traceLevel) ;

      //-------------------------------      

      butRefresh.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View v) 
         {
            //Log.d("TT2", GetLocalIpAddress()) ;
            
            int hackMode   = config.getHackMode() ;   
            int traceLevel = config.getTraceLevel() ; 
            showHackState(hackMode,traceLevel) ;
            
            // test
            /*

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
            printNetworkInfo (networkInfo) ;
            
            NetworkInfo networkInfoInstance = newNetWorkInfo(ConnectivityManager.TYPE_WIFI) ;
            printNetworkInfo (networkInfoInstance) ;
            
            Log.d("TT2","----------------") ;
            
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo[] allNetWorks = connMgr.getAllNetworkInfo() ;
            for (NetworkInfo item : allNetWorks)
               Controller.printNetworkInfo (item) ;

            Log.d("TT2","----------------") ;

            try {
               Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
               
               for (; en.hasMoreElements();) 
               {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) 
                           Log.d("TT2", "NetworkInterface : " + intf.toString() + " (isLoopbackAddress) ") ;
                        else
                           Log.d("TT2", "NetworkInterface : " + intf.toString() ) ;

                    }
                }
            } catch (SocketException ex) {
               Log.d("TT2","ERROR Obtaining IP. Exception : " + ex.getMessage());
            }

            Log.d("TT2","----------------") ;
            */
           
         }
      });     

      //-------------------------------

      // StrictMode allows to setup policies in your application to avoid doing incorrect things. 
      // As of Android 3.0 (Honeycomb) StrictMode is configured to crash with 
      // a NetworkOnMainThreadException exception, if network is accessed in the user interface thread.
      
      //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      //StrictMode.setThreadPolicy(policy);

      mUpdateTimeTask = new Runnable() 
      {
         public void run() {
            hackMode   = config.getHackMode() ;   // Controller.getFakeConnectionActivated() ;
            traceLevel = config.getTraceLevel() ; // Controller.getTraceLevel() ;
            showHackState(hackMode,traceLevel) ;
         }
         
      } ;
      
      //-------------------------------

      butActivateWifi.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) 
         {            
            config.setHackMode(1) ;            

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 1000);           
            
         }
      });     
         
      //-------------------------------

      butActivateMobile.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) 
         {            
            config.setHackMode(2) ;                

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 1000);           
            
         }
      });     
         
      //-------------------------------
      butDesactivateHack.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) 
         {
            config.setHackMode(0) ;                

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 1000);
         }
      });     
      
      //-------------------------------

      butActivateTraces.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) 
         {            
            config.setTraceLevel(1) ;

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 1000);            
         }
      });     
         
      //-------------------------------

      butDesactivateTraces.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) 
         {
            config.setTraceLevel(0) ;

            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 1000);
         }
      });     

     
      //-------------------------------
      
   }  // onCreate()

   // -----------------------------------------------------------------------------

   void showHackState(int fakeWifiActivated, int traceActivated)
   {
      String hackState = "" ;
      if (fakeWifiActivated == 0 )
         hackState = "Hack desactivated" ;
      else if (fakeWifiActivated == 1 )
         hackState = "Forced to Wifi" ;
      else if (fakeWifiActivated == 2 )
         hackState = "Forced to Mobile" ;
      else if (fakeWifiActivated == -1 )
         hackState = "Hack not installed" ;

      hackState = hackState + System.getProperty ("line.separator") ;  //  \n
      if (traceActivated == 0)
         hackState = hackState + "Traces desactivated" ;
      else if (traceActivated == 1)
         hackState = hackState + "Traces activated" ;
      
      hackStateValue.setText(hackState) ;
   }
   
   // -----------------------------------------------------------------------------

/*
   private void sendConnectedBroadcast(NetworkInfo info)
   {  
      Intent intent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);  
      intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);  
      intent.putExtra(ConnectivityManager.EXTRA_NETWORK_INFO, info);  
      sendStickyBroadcast(intent);  
      
      //ConnectivityManager.sendGeneralBroadcast(info, ConnectivityManager.CONNECTIVITY_ACTION);
      //ConnectivityManager.sendGeneralBroadcastDelayed(info, ConnectivityManager.CONNECTIVITY_ACTION, 100); // ms

      //if (info.isFailover()) {
      //   intent.putExtra(ConnectivityManager.EXTRA_IS_FAILOVER, true);
      //   info.setFailover(false);
      //}
      //if (info.getReason() != null) {
      //   intent.putExtra(ConnectivityManager.EXTRA_REASON, info.getReason());
      //}
      //if (info.getExtraInfo() != null) {
      //   intent.putExtra(ConnectivityManager.EXTRA_EXTRA_INFO,
      //         info.getExtraInfo());
      //}

   }  
*/
   // -----------------------------------------------------------------------------
   
/*
   public static Constructor<?> getCompatibleConstructor(Class<?> clazz, Class<?>[] parameterTypes)
   {
      Constructor<?>[] constructors = clazz.getConstructors();
      for (Constructor<?> constructor : constructors)
      {
         if (constructor.getParameterTypes().length == (parameterTypes != null ? parameterTypes.length : 0))
         {
            // If we have the same number of parameters there is a shot that we have a compatible
            // constructor
            Class<?>[] constructorTypes = constructor.getParameterTypes();
            boolean isCompatible = true;
            for (int j = 0; j < (parameterTypes != null ? parameterTypes.length : 0); j++)
            {
               if (!constructorTypes[j].isAssignableFrom(parameterTypes[j]))
               {
                  // The type is not assignment compatible, however
                  // we might be able to coerce from a basic type to a boxed type
                  if (constructorTypes[j].isPrimitive())
                  {
                     if (!isAssignablePrimitiveToBoxed(constructorTypes[j], parameterTypes[j]))
                     {
                        isCompatible = false;
                        break;
                     }
                  }
               }
            }
            if (isCompatible)
            {
               return constructor;
            }
         }
      }
      return null;
   }
*/
   // -----------------------------------------------------------------------------

/*
   private static boolean isAssignablePrimitiveToBoxed(Class<?> primitive, Class<?> boxed)
   {
      if (primitive.equals(java.lang.Boolean.TYPE))
      {
         if (boxed.equals(java.lang.Boolean.class))
            return true;
         else
            return false;
      }
      else
      {
         if (primitive.equals(java.lang.Byte.TYPE))
         {
            if (boxed.equals(java.lang.Byte.class))
               return true;
            else
               return false;
         }
         else
         {
            if (primitive.equals(java.lang.Character.TYPE))
            {
               if (boxed.equals(java.lang.Character.class))
                  return true;
               else
                  return false;
            }
            else
            {
               if (primitive.equals(java.lang.Double.TYPE))
               {
                  if (boxed.equals(java.lang.Double.class))
                     return true;
                  else
                     return false;
               }
               else
               {
                  if (primitive.equals(java.lang.Float.TYPE))
                  {
                     if (boxed.equals(java.lang.Float.class))
                        return true;
                     else
                        return false;
                  }
                  else
                  {
                     if (primitive.equals(java.lang.Integer.TYPE))
                     {
                        if (boxed.equals(java.lang.Integer.class))
                           return true;
                        else
                           return false;
                     }
                     else
                     {
                        if (primitive.equals(java.lang.Long.TYPE))
                        {
                           if (boxed.equals(java.lang.Long.class))
                              return true;
                           else
                              return false;
                        }
                        else
                        {
                           if (primitive.equals(java.lang.Short.TYPE))
                           {
                              if (boxed.equals(java.lang.Short.class))
                                 return true;
                              else
                                 return false;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      return false;
   }
*/
   // -----------------------------------------------------------------------------
/*
   public NetworkInfo newNetWorkInfo (int networkType)
   {
      Class<NetworkInfo>       networkinfoClass = NetworkInfo.class ;
      Constructor<NetworkInfo> networkinfoConstructor = null ;
      NetworkInfo              result = null ;
      
      Class[] cArg = new Class[1];
      cArg[0] = Integer.class;

      networkinfoConstructor = (Constructor<NetworkInfo>) getCompatibleConstructor(networkinfoClass, cArg) ;      
      if (networkinfoConstructor != null)
      {
         networkinfoConstructor.setAccessible(true) ;
         try {
            result = networkinfoConstructor.newInstance(networkType) ;
            
            XposedHelpers.setIntField    (result, "mNetworkType"   , networkType) ;
            XposedHelpers.setBooleanField(result, "mIsAvailable"   , true) ;
            XposedHelpers.setObjectField (result, "mState"         , State.CONNECTED) ;
            XposedHelpers.setObjectField (result, "mDetailedState" , DetailedState.CONNECTED) ;
            XposedHelpers.setObjectField(result, "mSubtypeName", "") ;
            if (networkType == ConnectivityManager.TYPE_WIFI)
               XposedHelpers.setObjectField (result, "mTypeName"      , "WIFI") ;
            else
               XposedHelpers.setObjectField (result, "mTypeName"      , "MOBILE") ;
            
         } catch (IllegalArgumentException e) {
            Log.e("TT2", "IllegalArgumentException") ;
         } catch (InstantiationException e) {
            Log.e("TT2", "InstantiationException") ;
         } catch (IllegalAccessException e) {
            Log.e("TT2", "IllegalAccessException") ;
         } catch (InvocationTargetException e) {
            Log.e("TT2", "InvocationTargetException") ;
         }               
      }
      return result ;
   }
*/
   // ---------------------------------------------------------------------------------------
   
   public void printNetworkInfo (NetworkInfo networkInfo )
   {
      if (networkInfo == null)
      {
         Log.d("TT2", "networkInfo is null");
      } else {
         StringBuilder builder = new StringBuilder("   type: ").append(networkInfo.getTypeName()).append("[").append(networkInfo.getSubtypeName()).
               append("], state: ").append(networkInfo.getState()).append("/").append(networkInfo.getDetailedState()).
               append(", isAvailable: ").append(networkInfo.isAvailable());
         Log.d("TT2",builder.toString());
      }
   }
  
   // ---------------------------------------------------------------------------------------


}
