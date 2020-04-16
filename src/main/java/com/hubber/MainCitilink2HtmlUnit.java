package com.hubber;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MainCitilink2HtmlUnit {
    private static final Logger logger = LoggerFactory.getLogger(MainCitilink2HtmlUnit.class);
    private static String WEBDRIVER_LOCATION = ".//drivers//chromedriver.exe";
    private static int captchaBypassCounter = 0;
    private static Scanner scanner;

    public static void main(String[] args) throws IOException {
        logger.trace("Method main(), entry point ->");
        scanner = new Scanner(System.in);

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
//        webClient.getCookieManager().clearCookies();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        setSystemPropertiesForSelenium();

//        test();
//        testDB();

        test2(webClient);

        DBUtils.shutdown();
        webClient.close();
        scanner.close();
        logger.trace("Method main(), exit point <-");
    }

    public static void testDB() {
        logger.trace("Method testDB(), entry point ->");
        DBUtils.testSave();
        logger.trace("Method testDB(), exit point <-");
    }

/*
    public static void test() {
        logger.trace("Method test(), entry point ->");
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--log-level=OFF");
//        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.ya.ru");
        String title = driver.getTitle();
        logger.debug("Method test(), title: {}", title);
        driver.quit();
        logger.trace("Method test(), exit point <-");
    }
*/

    public static void test2(WebClient webClient) throws IOException {
        logger.trace("Method test2(), entry point ->");
        List<String> listRootURLs = findRootURLs(webClient);
        List<String> listFinalURLs = new ArrayList<>();

        for (String rootURL : listRootURLs) {
            findFinalURLS(rootURL);
        }
        logger.trace("Method test2(), exit point <-");
    }

    private static List<String> findRootURLs(WebClient webClient) throws IOException {
        logger.trace("Method findRootURLs(), entry point ->");
        List<String> res = new ArrayList<>();

        HtmlPage page = webClient.getPage("https://www.citilink.ru/catalog");
        List<HtmlListItem> listItems = page.getByXPath("//ul[@class='category-catalog__children-list']/li");

        for (HtmlListItem item : listItems) {
            List<HtmlAnchor> listAnchors = item.getByXPath(".//a");
            String rootURL = listAnchors.get(0).getAttribute("href");
            res.add(rootURL);
            logger.debug("Method findRootURLs(), root URL found: " + rootURL);
        }
        logger.trace("Method findRootURLs(), exit point <-");

        return res;
    }

//Возвращает список финальных URL для заданного корневого URL.
//В HTML-странице корневого URL могут быть ссылки на дополнительные страницы со списками товаров.
//Цель этого метода - вернуть URL без ссылок на дополнительные страницы со списками товаров.
    private static List<String> findFinalURLS(String rootURL) throws IOException {
        logger.trace("Method findFinalURLS(), entry point ->");
        logger.info("Method findFinalURLS(), checking URL: {}", rootURL);
        List<String> res = new ArrayList<>();

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        HtmlPage page = webClient.getPage(rootURL);

        List<HtmlListItem> listItems = page.getByXPath("//ul[starts-with(@class, 'col subnavigation__left_side_block_ul')]/li");

        if (listItems.size() == 0) {
            //Найден финальный URL
            logger.debug("Method findFinalURLS(), final URL found: {}", rootURL);
            List<ProxyProduct> listProds = parseItems(rootURL, webClient);
            DBUtils.save(listProds);
            res.add(rootURL);
            webClient.close();
            logger.trace("Method findFinalURLS(), listItems.size() == 0, exit point <-");
            return res;
        }

        logger.debug("Method findFinalURLS(), intermediate URL: {}", rootURL);
        for (HtmlListItem item : listItems) {
            List<HtmlAnchor> listAnchors = item.getByXPath("./a");
            String currentURL = listAnchors.get(0).getAttribute("href");
            List<String> recursiveList = findFinalURLS(currentURL);
            res.addAll(recursiveList);
        }
        webClient.close();
        logger.trace("Method findFinalURLS(), exit point <-");

        return res;
    }

//Возвращает список товаров, найденных на странице с указанным URL.
//Метод сам осуществляет пэйджинацию(перелистываение) списка товаров, если список товаров разделен на группы.
    private static List<ProxyProduct> parseItems(String url, WebClient webClient) throws IOException {
        logger.trace("Method parseItems(), entry point ->");
        List<ProxyProduct> res = new ArrayList<>();
        HtmlPage page = checkForCaptcha(url, webClient);
        List<HtmlDivision> listDivs = page.getByXPath("//div[@class='js--subcategory-product-item subcategory-product-item product_data__gtm-js  product_data__pageevents-js ddl_product']");

        //Создаем объекты класса com.hubber.Product, забиваем туда description, price и т.д.
        //Все помещаем в список res
        //..
        for (HtmlDivision div : listDivs) {
            String jsonParams = div.getAttribute("data-params");
            ProxyProduct product = new ProxyProduct(jsonParams);
            res.add(product);
        }

        //Осуществляем пэйджинацию, при необходимости
        List<HtmlLink> pages = page.getByXPath("//div[@class='page_listing'][1]//link[@rel='next']");
        if (pages.size() == 0) {
            logger.trace("Method parseItems(), pages.size() == 0, exit point <-");
            return res;
        }
        res.addAll(parseItems(pages.get(0).getAttribute("href"), webClient));
        logger.trace("Method parseItems(), exit point <-");

        return res;
    }

    //Проверка на капчу и ее ручной ввод при необходимости
    private static HtmlPage checkForCaptcha(String url, WebClient webClient) throws IOException {
        logger.trace("Method checkForCaptcha(), entry point ->");

        HtmlPage page = webClient.getPage(url);
        int statusCode = page.getWebResponse().getStatusCode();
        if (statusCode != 429) {
            captchaBypassCounter = 0;
            logger.debug("Method checkForCaptcha(), captcha bypassed");
            logger.trace("Method checkForCaptcha(), statusCode != 429, exit point <-");
            return page;
        }

        //Требуется ввод капчи
        System.setProperty("webdriver.chrome.driver", WEBDRIVER_LOCATION);
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.get(page.getUrl().toString());

        try {
            if (captchaBypassCounter < 3) {
                captchaBypassCounter++;
                Thread.sleep(5000);
                WebElement captchaInput = driver.findElement(By.xpath("//input[@name='captcha']"));
                WebElement captchaButton = driver.findElement(By.xpath("//button[@type='submit']"));
                String captchaValue = "some text";
                captchaInput.sendKeys(captchaValue);
                captchaButton.click();
                logger.debug("Method checkForCaptcha(), captchaBypassCounter < 3");
            } else if (captchaBypassCounter >= 3) {
                captchaBypassCounter++;
                Thread.sleep(100000);
                WebElement captchaInput = driver.findElement(By.xpath("//input[@name='captcha']"));
                WebElement captchaButton = driver.findElement(By.xpath("//button[@type='submit']"));
                String captchaValue = "f";
                captchaInput.sendKeys(captchaValue);
                captchaButton.click();
                logger.debug("Method checkForCaptcha(), captchaBypassCounter >= 3");
            }
        } catch (Exception e) {
            logger.error("Method checkForCaptcha(), exception occurred", e);
            try {
                Thread.sleep(180000);
            } catch (InterruptedException interruptedException) {

            }
        }
        driver.close();
        driver.quit();
        logger.trace("Method checkForCaptcha(), exit point <-");

        return checkForCaptcha(url, webClient);

    }

    private static void setSystemPropertiesForSelenium() {
        logger.trace("Method setSystemPropertiesForSelenium(), entry point ->");
        //Вывод логов Selenium в файл и отключение вывода встандартные потоки вывода и ошибок
        System.setProperty("webdriver.chrome.driver", ".//drivers//chromedriver.exe");
        System.setProperty("webdriver.chrome.logfile", ".//logs//selenium.log");
//        System.setProperty("webdriver.chrome.verboselogging", "false");
//        System.setProperty("webdriver.chrome.silentOutput", "true");
//        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        logger.trace("Method setSystemPropertiesForSelenium(), exit point <-");
    }
}
