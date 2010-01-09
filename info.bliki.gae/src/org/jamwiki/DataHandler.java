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
package org.jamwiki;


import java.util.Locale;

import org.jamwiki.model.Topic;
import org.jamwiki.model.VirtualWiki;
import org.jamwiki.model.WikiUser;
import org.springframework.dao.DataAccessException;

/**
 * This interface provides all methods needed when retrieving or modifying
 * Wiki data.  Any database or other persistency class must implement
 * this interface, and there should also be a corresponding
 * &lt;data-handler&gt; entry for the class in the
 * <code>jamwiki-configuration.xml</code> file.
 *
 * @see org.jamwiki.WikiBase#getDataHandler
 */
public interface DataHandler {

	/** Ansi data handler class */
	public static final String DATA_HANDLER_ANSI = "org.jamwiki.db.AnsiDataHandler";
	/** DB2 data handler class */
	public static final String DATA_HANDLER_DB2 = "org.jamwiki.db.DB2DataHandler";
	/** DB2/400 data handler class */
	public static final String DATA_HANDLER_DB2400 = "org.jamwiki.db.DB2400DataHandler";
	/** HSql data handler class */
	public static final String DATA_HANDLER_HSQL = "org.jamwiki.db.HSqlDataHandler";
	/** MSSql data handler class */
	public static final String DATA_HANDLER_MSSQL = "org.jamwiki.db.MSSqlDataHandler";
	/** MySql data handler class */
	public static final String DATA_HANDLER_MYSQL = "org.jamwiki.db.MySqlDataHandler";
	/** Oracle data handler class */
	public static final String DATA_HANDLER_ORACLE = "org.jamwiki.db.OracleDataHandler";
	/** Postgres data handler class */
	public static final String DATA_HANDLER_POSTGRES = "org.jamwiki.db.PostgresDataHandler";
	/** Sybase ASA data handler class */
	public static final String DATA_HANDLER_ASA = "org.jamwiki.db.SybaseASADataHandler";

	/**
	 * Retrieve a Topic object that matches the given virtual wiki and topic
	 * name.
	 *
	 * @param virtualWiki The virtual wiki for the topic being queried.
	 * @param topicName The name of the topic being queried.
	 * @param deleteOK Set to <code>true</code> if deleted topics can be
	 *  retrieved, <code>false</code> otherwise.
	 * @param transactionObject If this method is being called as part of a
	 *  transaction then this parameter should contain the transaction object,
	 *  such as a database connection.  If this method is not part of a
	 *  transaction then this value should be <code>null</code>.
	 * @return A Topic object that matches the given virtual wiki and topic
	 *  name, or <code>null</code> if no matching topic exists.
	 * @throws DataAccessException Thrown if any error occurs during method execution.
	 */
	Topic lookupTopic(String virtualWiki, String topicName, boolean deleteOK, Object transactionObject);

	 /**
   * Perform any required setup steps for the DataHandler instance.
   *
   * @param locale The locale to be used when setting up the data handler
   *  instance.  This parameter will affect any messages or defaults used
   *  for the DataHandler.
   * @param user The admin user to use when creating default topics and
   *  other DataHandler parameters.
   * @param username The admin user's username (login).
   * @param encryptedPassword The admin user's encrypted password.  This value
   *  is only required when creating a new admin user.
   * @throws DataAccessException Thrown if any error occurs during method execution.
   * @throws WikiException Thrown if a setup failure occurs.
   */
  void setup(Locale locale, WikiUser user, String username, String encryptedPassword);

  /**
   * Given a virtual wiki name, return the corresponding VirtualWiki object.
   *
   * @param virtualWikiName The name of the VirtualWiki object that is
   *  being retrieved.
   * @return The VirtualWiki object that corresponds to the virtual wiki
   *  name being queried, or <code>null</code> if no matching VirtualWiki
   *  can be found.
   * @throws DataAccessException Thrown if any error occurs during method execution.
   */
  VirtualWiki lookupVirtualWiki(String virtualWikiName) throws DataAccessException;

}
