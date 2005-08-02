/*
*+
*  Name:
*     StcSearchLocation.c

*  Purpose:
*     JNI implementations of native methods of StcSearchLocation class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     1-AUG-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_StcSearchLocation.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_StcSearchLocation_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jRegion,      /* Encapsulated region */
   jobjectArray jCoords  /* Array of KeyMaps representing AstroCoords */
) {
   jniastConstructStc( env, this, jRegion, jCoords,
                       astStcSearchLocation );
}
