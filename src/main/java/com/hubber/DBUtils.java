package com.hubber;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DBUtils {
    private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        logger.trace("Method getSessionFactory(), entry point ->");
        if (sessionFactory == null) {
            registry = new StandardServiceRegistryBuilder().configure().build();
            MetadataSources sources = new MetadataSources(registry);
            Metadata metadata = sources.getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();
        }
        logger.trace("Method getSessionFactory(), exit point <-");
        return sessionFactory;
    }

    public static void shutdown() {
        logger.trace("Method shutdown(), entry point ->");
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            sessionFactory = null;
        }
        logger.trace("Method shutdown(), exit point <-");
    }

    public static void testSave() {
        logger.trace("Method testSave(), entry point ->");
        EntityProduct product1 = new EntityProduct("Test product 1", "190389");
        EntityProduct product2 = new EntityProduct("Test product 2", "189341");
        EntityCategory category1 = new EntityCategory();
        category1.setTitle("cat one");
        EntityCategory category2 = new EntityCategory();
        category2.setTitle("cat two");
        product1.setCategory(category1);
        product2.setCategory(category2);

        EntityHistory historyEntity1 = new EntityHistory();
        EntityHistory historyEntity2 = new EntityHistory();
        EntityHistory historyEntity3 = new EntityHistory();
        historyEntity1.setPrice(14.45f);
        historyEntity1.setTimestamp(LocalDateTime.parse("2020-03-16T20:22:12"));
        historyEntity2.setPrice(994.96f);
        historyEntity2.setTimestamp(LocalDateTime.parse("2020-03-12T11:11:45"));
        historyEntity3.setPrice(1333.78f);
        historyEntity3.setTimestamp(LocalDateTime.parse("2019-11-12T03:34:17"));

        Set<EntityHistory> set1 = new HashSet<>();
        Set<EntityHistory> set2 = new HashSet<>();
        set1.add(historyEntity1);
        set1.add(historyEntity2);
        set2.add(historyEntity3);
        product1.setHistoryEntitySet(set1);
        product2.setHistoryEntitySet(set2);

        Session session = getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.save(category1);
        session.save(category2);
        session.save(product1);
        session.save(product2);
        transaction.commit();
        session.close();
        shutdown();
        logger.trace("Method testSave(), exit point <-");
    }

    public static void testQuery() {
        logger.trace("Method testQuery(), entry point ->");
        Session session = getSessionFactory().openSession();
        List<EntityHistory> list = session.createQuery("from com.hubber.EntityHistory", EntityHistory.class).list();
//        list.forEach(s -> System.out.println(s.getId() + ";" + s.getDescription() + "; " + s.getPrice() + "; " + s.getArticle()));
        session.close();
        shutdown();
        logger.trace("Method testQuery, exit point <-");
    }

    public static void save(List<ProxyProduct> list) {
        logger.trace("Method save(), entry point ->");
        Session session = getSessionFactory().openSession();
        for (ProxyProduct proxyProduct : list) {
            proxyDivide(proxyProduct, session);
        }

        Transaction transaction = session.beginTransaction();
        transaction.commit();
        session.close();
        shutdown();
        logger.trace("Method save(), exit point <-");
    }

//Метод делит целостный объект, содержащий информацию о товаре, на три сущности, каждая из которых хранится в разных таблицах
    private static void proxyDivide(ProxyProduct proxyProduct, Session session) {
        logger.trace("Method proxyDivide(), entryPoint ->");
        logger.trace("Method proxyDivide(), proxyProduct: {}", proxyProduct);

        EntityCategory resultCategory;
        EntityProduct resultProduct;
        EntityHistory resultHistory;

        Query<EntityCategory> queryCategory = session.createQuery("from com.hubber.EntityCategory where " +
                "TITLE = :paramTitle", EntityCategory.class);
        queryCategory.setParameter("paramTitle", proxyProduct.getCategory());
        List<EntityCategory> listEntityCategory = queryCategory.list();
        logger.trace("Method proxyDivide(), database query for checking a category: {}", queryCategory.getQueryString());
        if (listEntityCategory.size() == 0) {
            //Категории в БД еще нет, ее надо добавить
            logger.debug("Method proxyDivide(), there is no such category in the database: {}", proxyProduct.getCategory());
            resultCategory = new EntityCategory(proxyProduct.getCategory());
            insertCategory(resultCategory, session);
        } else if (listEntityCategory.size() == 1) {
            //Категория в БД уже есть
            logger.trace("Method proxyDivide(), this category is already in the database: {}", proxyProduct.getCategory());
            resultCategory = listEntityCategory.get(0);
        } else {
            //В БД найдено более одной категории с одним названием. Это ненормально и требует отображения в логе ошибок
            //Вместе с тем, чтобы не выбрасывать exception, возьмем первую по счету категорию, найденную в таблице БД
            logger.warn("Method proxyDivide(), several identical categories found in the database: {}", proxyProduct.getCategory());
            resultCategory = listEntityCategory.get(0);
        }

        Query<EntityProduct> queryProduct = session.createQuery("from com.hubber.EntityProduct where " +
                "ARTICLE = :paramArticle and DESCRIPTION = :paramDescription", EntityProduct.class);
        queryProduct.setParameter("paramArticle", proxyProduct.getArticle());
        queryProduct.setParameter("paramDescription", proxyProduct.getDescription());
        List<EntityProduct> listEntityProduct = queryProduct.list();
        logger.trace("Method proxyDivide(), database query for checking a product: {}", queryProduct.getQueryString());
        if (listEntityProduct.size() == 0) {
            //Товара в БД еще нет, его надо добавить
            logger.debug("Method proxyDivide(), there is no such product in the database: {}", proxyProduct);
            resultProduct = new EntityProduct(proxyProduct.getDescription(), proxyProduct.getArticle());
            resultProduct.setCategory(resultCategory);
            insertProduct(resultProduct, session);
        } else if (listEntityProduct.size() == 1) {
            //Товар в БД уже есть
            logger.trace("Method proxyDivide(), this product is already in the database: {}", proxyProduct);
            resultProduct = listEntityProduct.get(0);
        } else {
            //В БД найдено более одного товара с одинаковыми названиями и артикулом. Это ненормально и требует отображения в логе ошибок
            logger.warn("Method proxyDivide(), several identical products found in the database: {}", proxyProduct);
            resultProduct = listEntityProduct.get(0);
        }

        Query<EntityHistory> queryHistory = session.createQuery("from com.hubber.EntityHistory where " +
                "PRODUCT_ID = :paramProduct order by TIMESTAMP desc", EntityHistory.class).setMaxResults(1);
        queryHistory.setParameter("paramProduct", resultProduct.getId());
        List<EntityHistory> listEntityHistory = queryHistory.list();
        logger.trace("Method proxyDivide, database query for checking a history record: {}", queryHistory.getQueryString());
        if (listEntityHistory.size() == 0) {
            //Записи о цене товара нет, добавляем ее
            logger.info("Method proxyDivide(), there are no records on the price of the product in the database: {}", proxyProduct);
            resultHistory = new EntityHistory(LocalDateTime.now(), proxyProduct.getPrice());
            resultHistory.setProduct(resultProduct);
            insertHistory(resultHistory, session);
        } else {
            //Запись о цене товара в таблице уже присутствует
            //Необходимо сравнить цену в таблице с текущей и внести запись, если текущая цена отличается от сохраненной
            if (!listEntityHistory.get(0).getPrice().equals(proxyProduct.getPrice())) {
                logger.info("Method proxyDivide(), the price of the product has changed: {}\n" +
                        "old price: {}, current price: {}", proxyProduct, listEntityHistory.get(0).getPrice(), proxyProduct.getPrice());
                resultHistory = new EntityHistory(LocalDateTime.now(), proxyProduct.getPrice());
                resultHistory.setProduct(resultProduct);
                insertHistory(resultHistory, session);
            } else {
                logger.debug("Method proxyDivide(), the price of the product remained the same: {}", proxyProduct);
            }
        }

        logger.trace("Method proxyDivide(), exit point <-");
    }

    private static void insertProduct(EntityProduct entityProduct, Session session) {
        logger.trace("Method insertProduct(), entry point ->");
        logger.debug("Method insertProduct(), adding a new EntityProduct: {}", entityProduct);
        Transaction transaction = session.beginTransaction();
        session.save(entityProduct);
        transaction.commit();
        logger.trace("Method insertProduct, exit point <-");
    }

    private static void insertCategory(EntityCategory entityCategory, Session session) {
        logger.trace("Method insertCategory(), entry point ->");
        logger.debug("Method insertCategory(), adding a new EntityCategory: {}", entityCategory);
        Transaction transaction = session.beginTransaction();
        session.save(entityCategory);
        transaction.commit();
        logger.trace("Method insertCategory(), exit point <-");
    }

    private static void insertHistory(EntityHistory entityHistory, Session session) {
        logger.trace("Method insertHistory(), entry point ->");
        logger.debug("Method insertHistory(), adding a new EntityHistory: {}", entityHistory);
        Transaction transaction = session.beginTransaction();
        session.save(entityHistory);
        transaction.commit();
        logger.trace("Method insertHistory(), exit point <-");
    }
}
