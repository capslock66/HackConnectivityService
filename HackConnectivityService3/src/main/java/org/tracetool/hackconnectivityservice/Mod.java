package org.tracetool.hackconnectivityservice;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class Mod implements IXposedHookZygoteInit, IXposedHookLoadPackage 
{

   // this var is dependant of the process
   static String savedProcessName = "" ;
   static LoadPackageParam _lpparam;

   // ---------------------------------------------------------------------------------------

   @Override
   public void initZygote(StartupParam startupParam) throws Throwable 
   {
      // Log.i("TT2","initZygote : modulePath : " + startupParam.modulePath) ;

      // Fake wifi is desactivated at startup.
      // packages are hooked, but do nothing special
      Controller.prefSaveHackMode(0) ;     

      // trap systemReady
      //--------------------------
      try 
      {
         hook_method("com.android.server.am.ActivityManagerService", null, "systemReady", Runnable.class, new XC_MethodHook()
         {
                  @Override
                  protected void beforeHookedMethod(final MethodHookParam param) throws Throwable 
                  {
                     // Log.i("TT2","system ready");
                     final Runnable origCallback = (Runnable) param.args[0];
                     param.args[0] = new Runnable()
                     {
                        @Override
                        public void run() {
                           origCallback.run();
                           Context mContext = (Context) getObjectField(param.thisObject, "mContext");

                           // registerReceiver
                           handleSystemServicesReady(mContext);
                        }
                     };
                  }
         });
      } catch (Throwable t) {
         Log.e("TT2","error getting ActivityManagerService.systemReady()");
         Log.i("TT2",t.getMessage()); 
      }

   }

   // ---------------------------------------------------------------------------------------

   // com.android.server.am.ActivityManagerService.systemReady
   public void handleSystemServicesReady(Context context) 
   {
   }

   // ---------------------------------------------------------------------------------------

   @Override
   public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
   {
      // Log.i("TT2","handleLoadPackage : packageName : " + lpparam.packageName) ;

      _lpparam = lpparam;
      if (savedProcessName.length() == 0)
         savedProcessName = lpparam.processName;

      // skip 3g Watchdog Process
      // if (lpparam.packageName.equals("net.rgruet.android.g3watchdog"))
      // {
      //    Log.i("TT2","skip g3watchdog") ;
      //    return ;
      // }

      // For the current package (lpparam.processName) hook these 4 methods in the android.net.ConnectivityManager class :
      // NetworkInfo getActiveNetworkInfo ()
      // NetworkInfo getActiveNetworkInfoForUid (int uid)
      // NetworkInfo getNetworkInfo (int networkType)
      // NetworkInfo[] getAllNetworkInfo ()

      Class<?> ConnectivityManagerClazz ;
      try {
         ConnectivityManagerClazz = de.robv.android.xposed.XposedHelpers.findClass("android.net.ConnectivityManager",lpparam.classLoader);
      } catch (Throwable t) {
         Log.e("TT2", "Unable to get ConnectivityManager");
         Log.e("TT2", t.getMessage());
         return; //
      }

      //-----------------------------

      // public NetworkInfo getActiveNetworkInfo()
      hook_method(
          ConnectivityManagerClazz, // class
          "getActiveNetworkInfo", // String methodName
          // no parameters
          new XC_MethodHook() 
          {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable 
               {
                  Controller.doit_networkinfo("ConnectivityManager.getActiveNetworkInfo()", param);
               }
         });

      // -----------------------------

      // public NetworkInfo getActiveNetworkInfoForUid(int uid)
      hook_method(
         ConnectivityManagerClazz, 
         "getActiveNetworkInfoForUid", 
         int.class, 
         new XC_MethodHook() 
         {
           @Override
           protected void afterHookedMethod(MethodHookParam param) throws Throwable 
           {
              Controller.doit_networkinfo("ConnectivityManager.getActiveNetworkInfoForUid(int)",param);
           }
      }); 

      //-----------------------------

      // getProvisioningOrActiveNetworkInfo()      UNDOCUMENTED
      hook_method(
            ConnectivityManagerClazz,
            "getProvisioningOrActiveNetworkInfo", 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {  
            Controller.doit_networkinfo("ConnectivityManager.getProvisioningOrActiveNetworkInfo()", param);   
         }
      });

      //-----------------------------

      /*
      // getActiveNetworkInfoUnfiltered()    UNDOCUMENTED
      hook_method(
            ConnectivityManagerClazz,
            "getActiveNetworkInfoUnfiltered", 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {  
            Controller.doit_networkinfo("ConnectivityManager.getActiveNetworkInfoUnfiltered()", param);   
         }
      });
      */

      //-----------------------------

      // public NetworkInfo[] getAllNetworkInfo()
      hook_method(    
            ConnectivityManagerClazz, 
            "getAllNetworkInfo", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.doit_allNetworkinfo("ConnectivityManager.getAllNetworkInfo()", param) ;
         }
      }); 

      //-----------------------------

      // public NetworkInfo getNetworkInfo(int networkType) 
      hook_method(   
            ConnectivityManagerClazz, 
            "getNetworkInfo", 
            int.class, 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.doit_networkinfo("ConnectivityManager.getNetworkInfo(int)", param) ;
         }
      }); 

      //-----------------------------

      // isActiveNetworkMetered()
      hook_method(
            ConnectivityManagerClazz,
            "isActiveNetworkMetered", 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {
            Controller.logHook("ConnectivityManager.isActiveNetworkMetered()");
            
            int fakeWifiActivated = Controller.prefGetHackMode() ;
            if (fakeWifiActivated == 0)
               return ;

            param.setResult(false);
         }
      });

      //-----------------------------

      // requestRouteToHost(int, int)     LOG ONLY
      hook_method(
            ConnectivityManagerClazz,
            "requestRouteToHost", 
            int.class, int.class, 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {
            int network_type = (Integer) param.args[0];
            int host_addr = (Integer) param.args[1];

            Controller.logHook("ConnectivityManager.requestRouteToHost(" + network_type + ", " + host_addr + ")");
         }
      });

      //-----------------------------

      // getActiveLinkProperties()     LOG ONLY
      hook_method(
            ConnectivityManagerClazz,
            "getActiveLinkProperties", 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {
            Controller.logHook("ConnectivityManager.getActiveLinkProperties()");
         }
      });

      //-----------------------------

      // getLinkProperties(int)        LOG ONLY
      hook_method(
            ConnectivityManagerClazz,
            "getLinkProperties", 
            int.class, 
            new XC_MethodHook()
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable
         {
            int network_type = (Integer) param.args[0];
            Controller.logHook("ConnectivityManager.getLinkProperties(" + network_type + ")");
         }
      });
      
      //--------------------------------------------------------------------------------------------------------

      // WifiManager targets:
      //   isWifiEnabled()      
      //   getWifiState()
      //   getConnectionInfo()
      //   createWifiLock(string)
      //   createWifiLock(int, string)
      //   getDhcpInfo
      //   getConfiguredNetworks()
      //      for WifiConfiguration ...

      Class<?> WifiManagerClazz ;
      try {
         WifiManagerClazz = de.robv.android.xposed.XposedHelpers.findClass("android.net.wifi.WifiManager", lpparam.classLoader) ;
      } catch (Throwable t) { 
         Log.e("TT2","Unable to get WifiManager"); 
         Log.e("TT2",t.getMessage()); 
         return ;
      }

      //-----------------------------

      // Boolean isWifiEnabled()
      hook_method(
            WifiManagerClazz, 
            "isWifiEnabled", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.logHook("WifiManager.isWifiEnabled()");
            
            int fakeWifiActivated = Controller.prefGetHackMode() ;
            if (fakeWifiActivated == 1)
               param.setResult(true);            
         }
      });

      //-----------------------------

      // int getWifiState()
      hook_method(
            WifiManagerClazz, 
            "getWifiState", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            // WIFI_STATE_DISABLED(1), WIFI_STATE_DISABLING(0), 
            // WIFI_STATE_ENABLED(3), WIFI_STATE_ENABLING(2), WIFI_STATE_UNKNOWN (4)
            
            Controller.logHook("WifiManager.getWifiState()");  
            
            int fakeWifiActivated = Controller.prefGetHackMode() ;
            if (fakeWifiActivated==1)        
               param.setResult(WifiManager.WIFI_STATE_ENABLED);            
         }
      });

      //-----------------------------

      // WifiInfo getConnectionInfo()
      hook_method(
            WifiManagerClazz, 
            "getConnectionInfo", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.logHook("WifiManager.getConnectionInfo()");

            int fakeWifiActivated = Controller.prefGetHackMode() ;
            if (fakeWifiActivated == 1)
               param.setResult(Controller.createWifiInfo(_lpparam));
            
         }
      });

      //-----------------------------
      
      // getDhcpInfo()
      hook_method(
            WifiManagerClazz, 
            "getDhcpInfo", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.logHook("WifiManager.getDhcpInfo()");

            int fakeWifiActivated = Controller.prefGetHackMode() ;
            if (fakeWifiActivated==1)        
               param.setResult(Controller.createDhcpInfo());
         }
      });

      //-----------------------------

      // WifiManager.WifiLock createWifiLock(string)
      hook_method(
            WifiManagerClazz, 
            "createWifiLock", 
            String.class, 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            WifiManager.WifiLock res = (WifiManager.WifiLock) param.getResult() ;
            Controller.logHook("WifiManager.createWifiLock(String) : " + res);
         }
      });

      //-----------------------------

      // WifiManager.WifiLock createWifiLock(int, string)
      hook_method(
            WifiManagerClazz, 
            "createWifiLock", 
            int.class, String.class, 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            WifiManager.WifiLock res = (WifiManager.WifiLock) param.getResult() ;
            Controller.logHook("WifiManager.createWifiLock(int, String) : " + res);
         }
      });
      
      //-----------------------------

      // List<WifiConfiguration> getConfiguredNetworks()
      hook_method(
            WifiManagerClazz, 
            "getConfiguredNetworks", 
            new XC_MethodHook() 
      {
         @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
            Controller.logHook("WifiManager.getConfiguredNetworks() : " + param.getResult());
         }
      });

      //------------------------------------------------------------------------------------------------------------------

      // java.net.NetworkInterface

      // public static NetworkInterface getByName(String interfaceName) throws SocketException
      // public static NetworkInterface getByInetAddress(InetAddress address) throws SocketException
      // public static NetworkInterface getByIndex(int index) throws SocketException
      // public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException
      // public List<InterfaceAddress> getInterfaceAddresses()
      // public Enumeration<NetworkInterface> getSubInterfaces()
      // public NetworkInterface getParent()
      // public boolean isUp() throws SocketException
      // public boolean isLoopback() throws SocketException
      // public boolean isPointToPoint() throws SocketException
      // public boolean supportsMulticast() throws SocketException
      // public byte[] getHardwareAddress() throws SocketException
      // public int getMTU() throws SocketException
      // public boolean isVirtual()

      //-----------------------------

      Class<?> configClazz ;

      try {
         configClazz = de.robv.android.xposed.XposedHelpers.findClass("org.tracetool.hackconnectivityservice.Config", lpparam.classLoader) ;
         Log.i("TT2",Mod.savedProcessName + " : " + "hackconnectivityservice.Config found"); 
      } catch (Throwable t) { 
         // Log.e("TT2",t.getMessage()); 
         //Log.i("TT2",Mod.savedProcessName + " : " + "Unable to get hackconnectivityservice.Config"); 
         return ;  // not an error
      }

      int traceActivated = Controller.prefGetTraceLevel() ;
      if (traceActivated >= 1)
      {

         if (lpparam.packageName.compareTo(lpparam.processName) == 0)      
            Log.i("TT2","handleLoadPackage : "
                  + lpparam.processName  /* The process in which the package is executed */
                  + " (" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ")") ;
         else
            Log.i("TT2","handleLoadPackage : " 
                  + lpparam.processName    /* The name of the package being loaded */
                  + " (" + android.os.Process.myPid() + "/" + android.os.Process.myTid() + ") " 
                  + lpparam.packageName) ;  /* The process in which the package is executed */
      }
      
      // hook these 4 methods in the org.tracetool.hackconnectivityservice.Config class :
      // getHackMode
      // setHackMode
      // getTraceLevel
      // setTraceLevel
      
      //-----------------------------

      // public int getHackMode()
      hook_method(   
            configClazz,                 // config class
            "getHackMode",               // String methodName
            // no parameters
            new XC_MethodHook()          // callback
            {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable 
               {
                  int newValue = Controller.prefGetHackMode() ;
                  param.setResult(newValue) ;
                  //Log.d("TT2", "Hooked getHackMode() call. Return " + newValue) ;
               }
            }); 

      //-----------------------------

      // public int setHackMode(int mode)
      hook_method(  
            configClazz,                             // config class
            "setHackMode", 
            int.class, 
            new XC_MethodHook() 
            {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable 
               {
                  Integer mode = (Integer) param.args[0] ;
                  Controller.prefSaveHackMode(mode) ;
                  int newValue = Controller.prefGetHackMode() ;
                  param.setResult(newValue) ;
                  //Log.d("TT2", "Hooked setHackMode (" + mode + ") call. Return " + newValue ) ;
               }
            }); 

      //-----------------------------

      // public int getTraceLevel()
      hook_method(   
            configClazz,                             // config class
            "getTraceLevel", 
            // no parameters
            new XC_MethodHook() 
            {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable 
               {
                  int newValue = Controller.prefGetTraceLevel() ;
                  param.setResult(newValue) ;
                  //Log.d("TT2", "Hooked getTraceLevel() call. Return " + newValue) ;
               }
            }); 

      //-----------------------------

      // public int setTraceLevel(int level)
      hook_method(   
            configClazz,                             // config class
            "setTraceLevel", 
            int.class, 
            new XC_MethodHook() 
            {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable 
               {
                  Integer level = (Integer) param.args[0] ;
                  Controller.prefSaveTraceLevel(level) ;
                  int newValue = Controller.prefGetTraceLevel() ;
                  param.setResult(newValue) ;
                  //Log.d("TT2", "Hooked setTraceLevel(" + level + ") call. Return " + newValue) ;
               }
            }); 
     
   }  // handleLoadPackage

   // ---------------------------------------------------------------------------------------

   // Same as XposedHelper's findAndHookMethod() but shows error msg instead of throwing exception (and returns void)
   private void hook_method(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)
   {
       try
       {   
          XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);  
       }
       catch (NoSuchMethodError e)
       {   
          Log.e("TT2","Error hooking method " + methodName);
          Log.e("TT2",e.getMessage());
       }
   }

   // idem
   private void hook_method(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)
   {
       try
       {   
          XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);  
       }
       catch (NoSuchMethodError e)
       {   
          Log.e("TT2","Error hooking method " + methodName);
          Log.e("TT2",e.getMessage());
       }
   }
}
