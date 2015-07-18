package org.tracetool.hackconnectivityservice;

// This class don't do something interresting.
// If the xposed HackConnectivityService.apk package module is installed correctly, all the functions in this class will be hooked by the module
// Simply copy this class in our code (with exact package name, class name and method signature) to configure the hack

public class Config 
{
   
   // connectivity hack mode : 
   //   -1 : undefined (no hack)
   //    0 : disabled
   //    1 : simulate Wifi
   //    2 : simulate 3G
   //    3 : ? other not yet handled interface
   
   // trace level :
   //   -1 : undefined (no hack)
   //    0 : disabled
   //    1 : level 1. 
   //    2 : ? not yet used
   
   //--------------------------------------------------------------------------------------------------

   // return the connectivity hack mode. 
   // If -1 is returned (default implementation) , the hack is not installed properly.
   public int getHackMode()
   {
      return -1 ;
   }

   //--------------------------------------------------------------------------------------------------

   // Set the connectivity kack mode (0..2)
   // If the hack is well installed, the mode parameter will be returned.
   // else -1 will be returned (default implementation).
   public int setHackMode(int mode)
   {
      return -1 ;
   }
   
   //--------------------------------------------------------------------------------------------------
   // Return the connectivity kack trace level (0..1)
   // If -1 is returned (default implementation) , the hack is not installed properly.

   public int getTraceLevel()
   {
      return -1 ;
   }

   //--------------------------------------------------------------------------------------------------

   // Set the connectivity kack trace level (0..1)
   // If the hack is well installed, the level parameter will be returned.
   // else -1 will be returned (default implementation).
   public int setTraceLevel(int level)
   {
      return -1 ;
   }
   
   //--------------------------------------------------------------------------------------------------
   
   
   
}
