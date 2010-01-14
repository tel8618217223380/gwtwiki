/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, version 2.1, dated February 1999.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the latest version of the GNU Lesser General
 * Public License as published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (LICENSE.txt); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jamwiki.servlets;

import info.bliki.gae.db.PageService;
import info.bliki.gae.db.WikiUserService;
import info.bliki.gae.utils.BlikiBase;
import info.bliki.gae.utils.BlikiUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jamwiki.Environment;
import org.jamwiki.WikiBase;
import org.jamwiki.WikiConfiguration;
import org.jamwiki.WikiException;
import org.jamwiki.WikiMessage;
import org.jamwiki.WikiVersion;
import org.jamwiki.model.Topic;
import org.jamwiki.model.WikiConfigurationObject;
import org.jamwiki.model.WikiUser;
import org.jamwiki.utils.Utilities;
import org.jamwiki.utils.WikiLogger;
import org.jamwiki.utils.WikiUtil;
import org.springframework.web.servlet.ModelAndView;

/**
 * Used to handle JAMWiki setup, including setting and validating JAMWiki
 * configuration values.
 * 
 * @see org.jamwiki.servlets.UpgradeServlet
 */
public class SetupServlet extends JAMWikiServlet {

  private static final WikiLogger logger = WikiLogger
      .getLogger(SetupServlet.class.getName());
  /** The name of the JSP file used to render the servlet output. */
  protected static final String JSP_SETUP = "setup.jsp";
  private static final int MINIMUM_JDK_VERSION = 150;

  private Topic setupSpecialWikiTopics(WikiUser wikiUser, Locale locale,
      String topicName) {
    // logger.info("Setting up special page " + virtualWiki + " / " +
    // topicName);
    // if (user == null) {
    // throw new IllegalArgumentException(
    // "Cannot pass null WikiUser object to setupSpecialPage");
    // }
    String contents = null;
    try {
      contents = BlikiUtil.readSpecialPage(locale, topicName);
      if (contents != null) {
        Topic page = new Topic(topicName, contents, wikiUser);
        page = PageService.save(page, null);
        return page;

      }
    } catch (IOException e) {
    }
    return null;
  }

  /**
   * This method handles the request after its parent class receives control.
   * 
   * @param request
   *          - Standard HttpServletRequest object.
   * @param response
   *          - Standard HttpServletResponse object.
   * @return A <code>ModelAndView</code> object to be handled by the rest of the
   *         Spring framework.
   */
  protected ModelAndView handleJAMWikiRequest(HttpServletRequest request,
      HttpServletResponse response, ModelAndView next, WikiPageInfo pageInfo)
      throws Exception {
    // if (!WikiUtil.isFirstUse()) {
    // throw new WikiException(new WikiMessage("setup.error.notrequired"));
    // }
    Topic page = PageService.findByTitle(BlikiBase.SPECIAL_PAGE_LEFT_MENU);
    if (page == null) {
      WikiUser wikiUser = WikiUserService.getWikiUser();
      // create special pages
      Locale locale = Locale.ENGLISH;
      setupSpecialWikiTopics(wikiUser, locale, BlikiBase.SPECIAL_PAGE_LEFT_MENU);
      setupSpecialWikiTopics(wikiUser, locale,
          BlikiBase.SPECIAL_PAGE_BOTTOM_AREA);
      setupSpecialWikiTopics(wikiUser, locale,
          BlikiBase.SPECIAL_PAGE_STYLESHEET);
      page = setupSpecialWikiTopics(wikiUser, locale,
          BlikiBase.SPECIAL_PAGE_STARTING_POINTS);
    }
    String function = (request.getParameter("function") == null) ? request
        .getParameter("override") : request.getParameter("function");
    if (function == null) {
      function = "";
    }
    try {
      if (!SystemUtils.isJavaVersionAtLeast(MINIMUM_JDK_VERSION)) {
        throw new WikiException(new WikiMessage("setup.error.jdk", Integer
            .valueOf(MINIMUM_JDK_VERSION).toString(), System
            .getProperty("java.version")));
      }
      if (!StringUtils.isBlank(function) && initialize(request, next, pageInfo)) {
        ServletUtil.redirect(next, WikiBase.DEFAULT_VWIKI, Environment
            .getValue(Environment.PROP_BASE_DEFAULT_TOPIC));
      } else {
        view(request, next, pageInfo);
      }
    } catch (Exception e) {
      handleSetupError(request, next, pageInfo, e);
    }
    return next;
  }

  /**
	 *
	 */
  private void handleSetupError(HttpServletRequest request, ModelAndView next,
      WikiPageInfo pageInfo, Exception e) {
    // reset properties
    Environment.setBooleanValue(Environment.PROP_BASE_INITIALIZED, false);
    if (!(e instanceof WikiException)) {
      logger.severe("Setup error", e);
    }
    try {
      this.view(request, next, pageInfo);
    } catch (Exception ex) {
      logger.severe("Unable to set up page view object for setup.jsp", ex);
    }
    if (e instanceof WikiException) {
      WikiException we = (WikiException) e;
      next.addObject("messageObject", we.getWikiMessage());
    } else {
      next.addObject("messageObject", new WikiMessage("error.unknown", e
          .getMessage()));
    }
  }

  /**
	 *
	 */
  protected void initParams() {
    this.layout = false;
    this.displayJSP = "setup";
  }

  /**
	 *
	 */
  private boolean initialize(HttpServletRequest request, ModelAndView next,
      WikiPageInfo pageInfo) throws Exception {
    setProperties(request, next);
    WikiUser user = setAdminUser(request);
    List<WikiMessage> errors = validate(request, user);
    if (!errors.isEmpty()) {
      this.view(request, next, pageInfo);
      next.addObject("errors", errors);
      next.addObject("username", user.getUsername());
      next.addObject("newPassword", request.getParameter("newPassword"));
      next
          .addObject("confirmPassword", request.getParameter("confirmPassword"));
      return false;
    }
    if (previousInstall() && request.getParameter("override") == null) {
      // user is trying to do a new install when a previous installation exists
      next.addObject("upgrade", "true");
      next.addObject("username", user.getUsername());
      next.addObject("newPassword", request.getParameter("newPassword"));
      next
          .addObject("confirmPassword", request.getParameter("confirmPassword"));
      return false;
    }
    Environment.setBooleanValue(Environment.PROP_BASE_INITIALIZED, true);
    Environment.setValue(Environment.PROP_BASE_WIKI_VERSION,
        WikiVersion.CURRENT_WIKI_VERSION);
    String username = request.getParameter("username");
    String newPassword = request.getParameter("newPassword");
    // String encryptedPassword = Encryption.encrypt(newPassword);
    // WikiBase.reset(request.getLocale(), user, username, encryptedPassword);
    // JAMWikiAuthenticationConfiguration.resetJamwikiAnonymousAuthorities();
    // JAMWikiAuthenticationConfiguration.resetDefaultGroupRoles();
    // Environment.saveProperties();
    // // the setup process does not add new topics to the index (currently)
    // // TODO - remove this once setup uses safe connection handling
    // WikiBase.getSearchEngine().refreshIndex();
    // // force current user credentials to be removed and re-validated.
    // SecurityContextHolder.clearContext();
    return true;
  }

  /**
	 *
	 */
  private boolean previousInstall() {
    String driver = Environment.getValue(Environment.PROP_DB_DRIVER);
    String url = Environment.getValue(Environment.PROP_DB_URL);
    String userName = Environment.getValue(Environment.PROP_DB_USERNAME);
    return false;
    // String password =
    // Encryption.getEncryptedProperty(Environment.PROP_DB_PASSWORD, null);
    // try {
    // DatabaseConnection.testDatabase(driver, url, userName, password, true);
    // } catch (Exception e) {
    // // no previous database, all good
    // return false;
    // }
    // return true;
  }

  /**
	 *
	 */
  private WikiUser setAdminUser(HttpServletRequest request) throws Exception {
    String username = request.getParameter("username");
    WikiUser user = new WikiUser(username);
    user.setCreateIpAddress(ServletUtil.getIpAddress(request));
    user.setLastLoginIpAddress(ServletUtil.getIpAddress(request));
    return user;
  }

  /**
	 *
	 */
  private void setProperties(HttpServletRequest request, ModelAndView next)
      throws Exception {
    Environment.setValue(Environment.PROP_BASE_FILE_DIR, request
        .getParameter(Environment.PROP_BASE_FILE_DIR));
    Environment.setValue(Environment.PROP_FILE_DIR_FULL_PATH, request
        .getParameter(Environment.PROP_FILE_DIR_FULL_PATH));
    Environment.setValue(Environment.PROP_FILE_DIR_RELATIVE_PATH, request
        .getParameter(Environment.PROP_FILE_DIR_RELATIVE_PATH));
    Environment.setValue(Environment.PROP_BASE_PERSISTENCE_TYPE, request
        .getParameter(Environment.PROP_BASE_PERSISTENCE_TYPE));
    if (Environment.getValue(Environment.PROP_BASE_PERSISTENCE_TYPE).equals(
        WikiBase.PERSISTENCE_EXTERNAL)) {
      Environment.setValue(Environment.PROP_DB_DRIVER, request
          .getParameter(Environment.PROP_DB_DRIVER));
      Environment.setValue(Environment.PROP_DB_TYPE, request
          .getParameter(Environment.PROP_DB_TYPE));
      Environment.setValue(Environment.PROP_DB_URL, request
          .getParameter(Environment.PROP_DB_URL));
      Environment.setValue(Environment.PROP_DB_USERNAME, request
          .getParameter(Environment.PROP_DB_USERNAME));
      // Encryption.setEncryptedProperty(Environment.PROP_DB_PASSWORD,
      // request.getParameter(Environment.PROP_DB_PASSWORD), null);
      next.addObject("dbPassword", request
          .getParameter(Environment.PROP_DB_PASSWORD));
      // } else {
      // WikiDatabase.setupDefaultDatabase(Environment.getInstance());
    }
    Environment.setValue(Environment.PROP_FILE_SERVER_URL, Utilities
        .getServerUrl(request));
    Environment.setValue(Environment.PROP_SERVER_URL, Utilities
        .getServerUrl(request));
  }

  /**
	 *
	 */
  private List<WikiMessage> validate(HttpServletRequest request, WikiUser user)
      throws Exception {
    // List<WikiMessage> errors =
    // ServletUtil.validateSystemSettings(Environment.getInstance());
    // if (StringUtils.isBlank(user.getUsername())) {
    // errors.add(new WikiMessage("error.loginempty"));
    // }
    // String newPassword = request.getParameter("newPassword");
    // String confirmPassword = request.getParameter("confirmPassword");
    // if (newPassword != null || confirmPassword != null) {
    // if (newPassword == null) {
    // errors.add(new WikiMessage("error.newpasswordempty"));
    // } else if (confirmPassword == null) {
    // errors.add(new WikiMessage("error.passwordconfirm"));
    // } else if (!newPassword.equals(confirmPassword)) {
    // errors.add(new WikiMessage("admin.message.passwordsnomatch"));
    // }
    // }
    // return errors;
    return null;
  }

  /**
	 *
	 */
  private void view(HttpServletRequest request, ModelAndView next,
      WikiPageInfo pageInfo) throws Exception {
    pageInfo.setContentJsp(JSP_SETUP);
    pageInfo.setSpecial(true);
    pageInfo.setPageTitle(new WikiMessage("setup.title",
        WikiVersion.CURRENT_WIKI_VERSION));
    List<WikiConfigurationObject> dataHandlers = WikiConfiguration
        .getInstance().getDataHandlers();
    next.addObject("dataHandlers", dataHandlers);
    WikiMessage logMessage = new WikiMessage("setup.help.logfile", WikiLogger
        .getDefaultLogFile(), WikiLogger.getLogConfigFile());
    next.addObject("logMessage", logMessage);
  }
}
