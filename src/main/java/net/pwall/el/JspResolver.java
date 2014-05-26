/*
 * @(#) JspResolver.java
 *
 * JSTL Expression Language Parser / Evaluator - JSP Name Resolver
 * Copyright (C) 2003, 2006, 2007, 2014  Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.el;

import javax.servlet.jsp.PageContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Name Resolver for Expression Language under JSP.  It will resolve all of the
 * built-in names specified in JSTL, and it will pick up user-specified names
 * form the <code>PageContext</code>.  Most of the built-in names resolve to an
 * object that implements the <code>Map</code> interface, since this allows
 * the use of the dot operator to access individual properties.
 *
 * @author <a href="mailto:pwall@pwall.net">Peter Wall</a>
 */
public class JspResolver implements Expression.Resolver {

    protected PageContext pageContext;
    private PageMap pageMap;
    private RequestMap requestMap;
    private SessionMap sessionMap;
    private ApplicationMap applicationMap;
    private ParamMap paramMap;
    private ParamValuesMap paramValuesMap;
    private HeaderMap headerMap;
    private HeaderValuesMap headerValuesMap;
    private InitParamMap initParamMap;
    private CookieMap cookieMap;

    /**
     * Construct the <code>JspResolver</code> object.
     *
     * @param pageContext  the JSP page context
     */
    public JspResolver(PageContext pageContext) {
        this.pageContext = pageContext;
        pageMap = null;
        requestMap = null;
        sessionMap = null;
        applicationMap = null;
        paramMap = null;
        paramValuesMap = null;
        headerMap = null;
        headerValuesMap = null;
        initParamMap = null;
        cookieMap = null;
    }

    /**
     * Resolve an identifier to a variable.
     *
     * @param identifier  the identifier to be resolved
     * @return            a variable, or null if the name can not be resolved
     */
    @Override
    public Expression resolve(String identifier) {
        Object result = null;
        if ("pageContext".equals(identifier)) {
            result = pageContext;
        }
        else if ("pageScope".equals(identifier)) {
            if (pageMap == null)
                pageMap = new PageMap();
            result = pageMap;
        }
        else if ("requestScope".equals(identifier)) {
            if (requestMap == null)
                requestMap = new RequestMap();
            result = requestMap;
        }
        else if ("sessionScope".equals(identifier)) {
            if (sessionMap == null)
                sessionMap = new SessionMap();
            result = sessionMap;
        }
        else if ("applicationScope".equals(identifier)) {
            if (applicationMap == null)
                applicationMap = new ApplicationMap();
            result = applicationMap;
        }
        else if ("param".equals(identifier)) {
            if (paramMap == null)
                paramMap = new ParamMap();
            result = paramMap;
        }
        else if ("paramValues".equals(identifier)) {
            if (paramValuesMap == null)
                paramValuesMap = new ParamValuesMap();
            result = paramValuesMap;
        }
        else if ("header".equals(identifier)) {
            if (headerMap == null)
                headerMap = new HeaderMap();
            result = headerMap;
        }
        else if ("headerValues".equals(identifier)) {
            if (headerValuesMap == null)
                headerValuesMap = new HeaderValuesMap();
            result = headerValuesMap;
        }
        else if ("initParam".equals(identifier)) {
            if (initParamMap == null)
                initParamMap = new InitParamMap();
            result = initParamMap;
        }
        else if ("cookie".equals(identifier)) {
            if (cookieMap == null)
                cookieMap = new CookieMap();
            result = cookieMap;
        }
        else {
            // attempt to resolve name in any scope
            result = pageContext.findAttribute(identifier);
            if (result == null)
                return null;
        }
        return new SimpleVariable(identifier, result);
    }

    protected class PageMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getAttribute((String)key,
                    PageContext.PAGE_SCOPE);
        }

    }

    protected class RequestMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getAttribute((String)key,
                    PageContext.REQUEST_SCOPE);
        }

    }

    protected class SessionMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getAttribute((String)key,
                    PageContext.SESSION_SCOPE);
        }

    }

    protected class ApplicationMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getAttribute((String)key,
                    PageContext.APPLICATION_SCOPE);
        }

    }

    protected class ParamMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getRequest().getParameter((String)key);
        }

    }

    protected class ParamValuesMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getRequest().getParameterValues((String)key);
        }

    }

    protected class HeaderMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return ((HttpServletRequest)pageContext.getRequest()).
                    getHeader((String)key);
        }

    }

    protected class HeaderValuesMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return ((HttpServletRequest)pageContext.getRequest()).
                    getHeaders((String)key);
        }

    }

    protected class InitParamMap extends DummyMap {

        @Override
        public Object get(Object key) {
            return pageContext.getServletContext().
                    getInitParameter((String)key);
        }

    }

    protected class CookieMap extends DummyMap {

        @Override
        public Object get(Object key) {
            String name = (String)key;
            Cookie[] cookies =
                    ((HttpServletRequest)pageContext.getRequest()).getCookies();
            int n = cookies.length;
            for (int i = 0; i < n; ++i) {
                Cookie cookie = cookies[i];
                if (cookie.getName().equals(name))
                    return cookie;
            }
            return null;
        }

    }

}
