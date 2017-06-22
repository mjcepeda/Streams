package edu.rit.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.derby.impl.jdbc.EmbedConnection;
import org.apache.derby.impl.jdbc.EmbedPreparedStatement42;
import org.apache.derby.impl.jdbc.EmbedResultSet42;

import edu.rit.dao.iapi.Database;
import edu.rit.dao.iapi.relational.UnaryOperation;
import edu.rit.dao.impl.store.access.TableDescriptor;

/**
 * The Class DerbyDBImpl.
 *
 * @author Maria J. Cepeda
 */
public class DerbyDBImpl implements Database {

	/** The db URL. */
	private static String dbURL = "jdbc:derby:memory:tempDB;create=true";
	
	/** The conn. */
	// jdbc Connection
	private static Connection conn = null;

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#createDB()
	 */
	@Override
	public void createDB() {
		try {
			//System.setProperty("derby.language.logQueryPlan", "true");
			// Get a connection
			conn = DriverManager.getConnection(dbURL);
		} catch (Exception except) {
			except.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#dropDB()
	 */
	@Override
	public void dropDB() {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
			String connectionURL = "jdbc:derby:memory:tempDB;drop=true";
			DriverManager.getConnection(connectionURL);
		} catch (SQLException se) {

			if (((se.getErrorCode() == 45000) && ("08006".equals(se.getSQLState())))) {
				// we got the expected exception
				System.out.println("Database drop normally");
				// Note that for single database shutdown, the expected
				// SQL state is "08006", and the error code is 45000.
			} else {
				// if the error code or SQLState is different, we have
				// an unexpected exception (shutdown failed)
				System.err.println("Database did not drop normally");
				printSQLException(se);
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#createTable(java.lang.String, java.util.Map)
	 */
	@Override
	public boolean createTable(String tableName, Map<String, String> columnsDescMap) {
		boolean isCreated = Boolean.FALSE;
		TableDescriptor tableDescriptor = null;
		// TODO MJCG Translate this code to Java 8
		StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
		for (Iterator<String> iter = columnsDescMap.keySet().iterator(); iter.hasNext();) {
			String columnName = iter.next();
			sql.append(columnName);
			sql.append(" " + columnsDescMap.get(columnName));
			if (iter.hasNext()) {
				sql.append(",");
			}

		}
		sql.append(")");
		//Statement s = null;
		PreparedStatement preparedStatement= null;
		try {
			/*s = conn.createStatement();
			isCreated = s.execute(sql.toString());
			*/
			preparedStatement = conn.prepareStatement(sql.toString());
			// execute create SQL statement
			preparedStatement.executeUpdate();
			
			//TODO MJCG get statement -> activation -> ac -> ddlTableDescriptor
			//extract columns description and table name

			EmbedPreparedStatement42 newStmt = (EmbedPreparedStatement42)preparedStatement;
			
			if (preparedStatement != null) {
				//preparedStatement
			}
			
		} catch (SQLException e) {
			System.err.println("Error creating table");
			printSQLException(e);
		} finally {
			// release all open resources to avoid unnecessary memory usage
			try {
				/*if (s != null) {
					s.close();
					s = null;
				}*/
				if (preparedStatement!= null) {
					preparedStatement.close();
					preparedStatement = null;
				}
			} catch (SQLException sqle) {
				printSQLException(sqle);
			}
		}
		return isCreated;
	}

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#insertData(java.lang.String, java.util.List)
	 */
	@Override
	public int[] insertData(String tableName, final List<Map<String, Object>> dataList) {
		int affectedRows[] = null;
		// TODO MJCG Translate this code to Java 8
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		StringBuilder placeholders = new StringBuilder();

		// get the first map from the list to create the insert query
		Map<String, Object> dataMap = dataList.stream().findFirst().get();
		for (Iterator<String> iter = dataMap.keySet().iterator(); iter.hasNext();) {
			sql.append(iter.next());
			placeholders.append("?");

			if (iter.hasNext()) {
				sql.append(",");
				placeholders.append(",");
			}
		}

		sql.append(") VALUES (").append(placeholders).append(")");

		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn.prepareStatement(sql.toString());

			for (Map<String, Object> m : dataList) {
				int i = 1;
				for (Object value : m.values()) {
					preparedStatement.setObject(i++, value);
				}
				preparedStatement.addBatch();
			}

			affectedRows = preparedStatement.executeBatch();

		} catch (SQLException e) {
			System.err.println("Error inserting data");
			printSQLException(e);
		} finally {
			// release all open resources to avoid unnecessary memory usage
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
					preparedStatement = null;
				}
			} catch (SQLException sqle) {
				printSQLException(sqle);
			}
		}
		return affectedRows;
	}

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#shutdown()
	 */
	@Override
	public void shutdown() {
		try {
			// the shutdown=true attribute shuts down Derby
			DriverManager.getConnection("jdbc:derby:;shutdown=true");

			// To shut down a specific database only, but keep the
			// engine running (for example for connecting to other
			// databases), specify a database in the connection URL:
			// DriverManager.getConnection("jdbc:derby:" + dbName +
			// ";shutdown=true");
		} catch (SQLException se) {
			if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
				// we got the expected exception
				System.out.println("Derby shut down normally");
				// Note that for single database shutdown, the expected
				// SQL state is "08006", and the error code is 45000.
			} else {
				// if the error code or SQLState is different, we have
				// an unexpected exception (shutdown failed)
				System.err.println("Derby did not shut down normally");
				printSQLException(se);
			}
		}

	}

	/**
	 * Prints details of an SQLException chain to <code>System.err</code>.
	 * Details included are SQL State, Error code, Exception message.
	 *
	 * @param e
	 *            the SQLException from which to print details.
	 */
	public static void printSQLException(SQLException e) {
		// Unwraps the entire exception chain to unveil the real cause of the
		// Exception.
		while (e != null) {
			System.err.println("\n----- SQLException -----");
			System.err.println("  SQL State:  " + e.getSQLState());
			System.err.println("  Error Code: " + e.getErrorCode());
			System.err.println("  Message:    " + e.getMessage());
			// for stack traces, refer to derby.log or uncomment this:
			// e.printStackTrace(System.err);
			e = e.getNextException();
		}
	}

	/* (non-Javadoc)
	 * @see edu.rit.dao.Database#getExecutionPlan(java.lang.String)
	 */
	public UnaryOperation getExecutionPlan(String query) {
		PreparedStatement preparedStatement = null;
		UnaryOperation executionPlan=null;
		try {
			//TODO MJCG Method in progress
			if (!(conn instanceof EmbedConnection))
				return null;
			
			EmbedConnection econn = (EmbedConnection) conn;
			econn.getLanguageConnection().setRunTimeStatisticsMode(Boolean.TRUE);
			preparedStatement = conn.prepareStatement(query);
			ParameterMetaData pmd = preparedStatement.getParameterMetaData();
			ResultSet rs = preparedStatement.executeQuery();
			
			EmbedResultSet42 newRS = (EmbedResultSet42) rs;
			
			ResultSetMetaData md = rs.getMetaData();
			rs.close();
			
			/*RunTimeStatisticsImpl rts = (RunTimeStatisticsImpl)econn.getLanguageConnection().getRunTimeStatisticsObject();
			Field field = rts.getClass().getDeclaredField("topResultSetStatistics");
			field.setAccessible(true);
			
			resultSetStatistics= (ResultSetStatistics) field.get(rts);
			*/
			/*System.out.println("-----------------Statement text------------------");
			System.out.println(rts.getStatementText());
			System.out.println("-----------------Statement Execution Plan text------------------");
			System.out.println(rts.getStatementExecutionPlanText());
			*/
		} catch (SQLException e) {
			printSQLException(e);
		} /*catch (NoSuchFieldException e) {
			System.err.println(e.getMessage());
		}*/ catch (SecurityException e) {
			System.err.println(e.getMessage());
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		} /*catch (IllegalAccessException e) {
			System.err.println(e.getMessage());
		} */finally {
			// release all open resources to avoid unnecessary memory usage
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
					preparedStatement = null;
				}
			} catch (SQLException sqle) {
				printSQLException(sqle);
			}
		}

		return executionPlan;
	}
}
