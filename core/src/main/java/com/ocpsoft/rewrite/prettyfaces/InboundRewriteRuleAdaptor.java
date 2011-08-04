/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.ocpsoft.rewrite.prettyfaces;

import com.ocpsoft.pretty.faces.config.rewrite.Redirect;
import com.ocpsoft.pretty.faces.config.rewrite.RewriteRule;
import com.ocpsoft.pretty.faces.rewrite.RewriteEngine;
import com.ocpsoft.pretty.faces.url.QueryString;
import com.ocpsoft.pretty.faces.url.URL;
import com.ocpsoft.pretty.faces.util.StringUtils;
import com.ocpsoft.rewrite.EvaluationContext;
import com.ocpsoft.rewrite.config.Condition;
import com.ocpsoft.rewrite.config.Operation;
import com.ocpsoft.rewrite.config.Rule;
import com.ocpsoft.rewrite.event.InboundRewrite;
import com.ocpsoft.rewrite.servlet.config.HttpCondition;
import com.ocpsoft.rewrite.servlet.config.HttpOperation;
import com.ocpsoft.rewrite.servlet.http.event.HttpInboundServletRewrite;
import com.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class InboundRewriteRuleAdaptor implements Rule
{
   private final RewriteRule rule;

   public InboundRewriteRuleAdaptor(final RewriteRule rule)
   {
      this.rule = rule;
   }

   public Condition getCondition()
   {
      return new HttpCondition() {

         @Override
         public boolean evaluateHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            if ((event instanceof InboundRewrite)
                     && rule.isInbound()
                     && rule.matches(new URL(event.getRequestURL()).decode().toURL()
                              + QueryString.build(event.getRequestQueryStringSeparator()
                                       + event.getRequestQueryString()).toQueryString()))
            {
               return true;
            }
            return false;
         }
      };
   }

   public Operation getOperation()
   {
      return new HttpOperation() {

         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            RewriteEngine engine = new RewriteEngine();
            String originalUrl = new URL(event.getRequestURL()).decode().toURL()
                     + QueryString.build(event.getRequestQueryStringSeparator()
                              + event.getRequestQueryString()).toQueryString();
            String newUrl = engine.processInbound(event.getRequest(), event.getResponse(), rule, originalUrl);

            if (!Redirect.CHAIN.equals(rule.getRedirect()))
            {
               /*
                * An HTTP redirect has been triggered; issue one if we have a URL or if the current URL has been
                * modified.
                */

               String redirectURL = null;

               /*
                * The rewrite changed the URL and no 'url' attribute has been set for the rule.
                */
               if (StringUtils.isBlank(rule.getUrl()) && !originalUrl.equals(newUrl))
               {

                  /*
                   * Add context path and encode request using encodeRedirectURL().
                   */
                  redirectURL = event.getContextPath() + newUrl;
               }
               else if (StringUtils.isNotBlank(rule.getUrl()))
               {
                  /*
                   * This is a custom location - don't call encodeRedirectURL() and don't add context path, just
                   * redirect to the encoded URL
                   */
                  redirectURL = newUrl.trim();
               }

               if (redirectURL != null)
               {
                  if (Redirect.PERMANENT.equals(rule.getRedirect()))
                     ((HttpInboundServletRewrite) event).redirectPermanent(redirectURL);
                  if (Redirect.TEMPORARY.equals(rule.getRedirect()))
                     ((HttpInboundServletRewrite) event).redirectTemporary(redirectURL);
               }
            }
            else {
               if (!originalUrl.equals(newUrl))
               {
                  ((HttpInboundServletRewrite) event).forward(newUrl);
               }
            }
         }

      };
   }
}
