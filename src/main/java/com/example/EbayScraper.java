package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EbayScraper {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "DB_webscrap";
    private static final String USER = "root"; // Change this
    private static final String PASSWORD = "placita"; // Change this

    public static void main(String[] args) {
        // Set the path to your ChromeDriver executable
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver"); // Change this

        // Create the database and table if they do not exist
        createDatabaseAndTable();
        System.out.println("Process starting...");

        // Create a new instance of ChromeDriver
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.ebay.com");

        // Perform the search (you can customize this part)
        WebElement searchBox = driver.findElement(By.id("gh-ac"));
        searchBox.sendKeys("laptop");
        searchBox.submit();

        // Wait for the page to load
        try {
            Thread.sleep(3000); // Simple wait (not recommended for production)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Item> items = new ArrayList<>();
        List<WebElement> itemElements = driver.findElements(By.cssSelector(".s-item__info.clearfix"));

        for (WebElement itemElement : itemElements) {
            try {
                String name = itemElement.findElement(By.cssSelector(".s-item__title")).getText();
                String price = itemElement.findElement(By.cssSelector(".s-item__price")).getText();
                String link = itemElement.findElement(By.cssSelector(".s-item__link")).getAttribute("href");

                items.add(new Item(name, price, link));
                storeInDatabase(name, price, link);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        driver.quit();

        // Display the results in a GUI table
        SwingUtilities.invokeLater(() -> createAndShowGUI(items));
    }

    private static void createDatabaseAndTable() {
        boolean databaseCreated = false;
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {

            // Create the database if it doesn't exist
            try {
                statement.executeUpdate("CREATE DATABASE " + DB_NAME);
                databaseCreated = true; // Database was created
            } catch (SQLException e) {
                // If the error code indicates that the database already exists, do nothing
                if (e.getErrorCode() != 1007) { // MySQL error code for "database exists"
                    throw e; // Rethrow if it's a different error
                }
            }

            if (databaseCreated) {
                System.out.println("Database created successfully.");
            } else {
                System.out.println("Database already exists.");
            }

            // Switch to the created database
            statement.executeUpdate("USE " + DB_NAME);

            // Create the table with an updated 'price' column size
            String createTableSQL = "CREATE TABLE IF NOT EXISTS items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "price VARCHAR(255) NOT NULL," + // Increased size of 'price' column
                    "link VARCHAR(2048) NOT NULL" +
                    ")";
            statement.executeUpdate(createTableSQL);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void storeInDatabase(String name, String price, String link) {
        try (Connection connection = DriverManager
                .getConnection(DB_URL + DB_NAME, USER, PASSWORD)) {
            String query = "INSERT INTO items (name, price, link) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, price);
            preparedStatement.setString(3, link);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI(List<Item> items) {
        JFrame frame = new JFrame("eBay Scraper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        String[] columnNames = {"Item Name", "Price", "Link"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);

        for (Item item : items) {
            model.addRow(new Object[]{item.getName(), item.getPrice(), item.getLink()});
        }

        JScrollPane scrollPane = new JScrollPane(table);

        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);

    }
}