package models;

import play.db.DB;

import java.sql.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Riboni1989
 * Date: 28/10/13
 * Time: 14:51
 * To change this template use File | Settings | File Templates.
 */
public class IsOnline {



    public static void checkOnline(){

        Date ora;
        Date utilDate;

        try{
            Connection connection = DB.getConnection();
            String query = "SELECT * FROM USERS WHERE ONLINE=? ";
            String query1 = "UPDATE USERS SET ONLINE = ? WHERE NAME = ? ";

            PreparedStatement statement = connection.prepareStatement(query);
            PreparedStatement statement1 = connection.prepareStatement(query1);


            statement.setBoolean(1, true);
            ResultSet rs = statement.executeQuery();

            if(rs.next()){
                ora = new Date();
                utilDate = rs.getTimestamp("LAST_ACTIVE");

                if(ora.getTime() - utilDate.getTime() >= 1000*60*2 ){                //1 sec X 60 sec X 2 = 2 minuti

                    try{

                        statement1.setBoolean(1, false);
                        statement1.setString(2, rs.getString("NAME"));
                        statement1.executeUpdate();

                    }
                    catch(SQLException ex){

                    }
                }


            }

            connection.close();

        }
        catch(SQLException ex){

        }
    }

    public static void keepOnline(String name){

        Date utilDate = new Date();
        Timestamp active = new Timestamp(utilDate.getTime());

        try{
            Connection connection = DB.getConnection();
            String query = "UPDATE USERS SET LAST_ACTIVE = ? WHERE NAME = ? ";

            PreparedStatement statement = connection.prepareStatement(query);

            statement.setTimestamp(1, active);
            statement.setString(2, name);
            statement.executeUpdate();

            connection.close();
        }
        catch(SQLException ex){

        }
    }

    public static void putOffline(String name){

        try{
            Connection connection = DB.getConnection();
            String query = "UPDATE USERS SET ONLINE = ? WHERE NAME = ? ";

            PreparedStatement statement = connection.prepareStatement(query);

            statement.setBoolean(1, false);
            statement.setString(2, name);
            statement.executeUpdate();

            connection.close();
        }
        catch(SQLException ex){

        }

    }



}
