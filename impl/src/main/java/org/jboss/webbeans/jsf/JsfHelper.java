/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.webbeans.jsf;

import javax.enterprise.inject.AnnotationLiteral;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.Container;
import org.jboss.webbeans.conversation.ConversationIdName;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;
import org.jboss.webbeans.servlet.ServletHelper;
import org.jboss.webbeans.util.Reflections;

/**
 * Helper class for JSF related operations
 * 
 * @author Nicklas Karlsson
 * @author Dan Allen
 */
public class JsfHelper
{
   private static LogProvider log = Logging.getLogProvider(JsfHelper.class);
   
   /**
    * Checks if the current request is a JSF postback. The JsfApiAbstraction is
    * consulted to determine if the JSF version is compatible with JSF 2.0. If
    * so, the {@link FacesContext#isPostback()} convenience method is used
    * (which is technically an optimized and safer implementation). Otherwise,
    * the ResponseStateManager is consulted directly.
    * 
    * @return true if this request is a JSF postback, false otherwise
    */
   public static boolean isPostback(FacesContext facesContext)
   {
      if (Container.instance().deploymentServices().get(JsfApiAbstraction.class).isApiVersionCompatibleWith(2.0))
      {
         return (Boolean) Reflections.invokeAndWrap("isPostback", facesContext);
      }
      else
      {
         return facesContext.getRenderKit().getResponseStateManager().isPostback(facesContext);
      }
   }

   /**
    * Gets the propagated conversation id parameter from the request
    * 
    * @return The conversation id (or null if not found)
    */
   public static String getConversationIdFromRequest(FacesContext facesContext)
   {
      BeanManagerImpl moduleBeanManager = JsfHelper.getModuleBeanManager(facesContext);
      String cidName = moduleBeanManager.getInstanceByType(String.class, new AnnotationLiteral<ConversationIdName>(){});
      String cid = facesContext.getExternalContext().getRequestParameterMap().get(cidName);
      log.trace("Found conversation id " + cid + " in request parameter");
      return cid;
   }

   /**
    * Gets the propagated conversation id.
    * 
    * @return The conversation id (or null if not found)
    */
   public static String getConversationId(FacesContext facesContext)
   {
      String cid = getConversationIdFromRequest(facesContext);
      log.debug("Resuming conversation with id " + cid);
      return cid;
   }
   
   /**
    * Gets the HTTP session
    * 
    * @return The session
    */
   public static HttpSession getHttpSession(FacesContext facesContext)
   {
      Object session = facesContext.getExternalContext().getSession(true);
      if (session instanceof HttpSession)
      {
         return (HttpSession) session;
      }
      else
      {
         return null;
      }
   }
   
   public static BeanManagerImpl getModuleBeanManager(FacesContext facesContext)
   {
      if (facesContext.getExternalContext().getContext() instanceof ServletContext)
      {
         return ServletHelper.getModuleBeanManager((ServletContext) facesContext.getExternalContext().getContext());
      }
      else
      {
         throw new IllegalStateException("Web Beans doesn not support using JSF in an non-servlet environment");
      }
   }

}