package org.nem.deploy;

import com.googlecode.flyway.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.nis.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Account;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.poi.PoiAlphaImportanceGeneratorImpl;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {"org.nem.nis"}, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class)
})
@EnableTransactionManagement
public class NisAppConfig {

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private BlockChainLastBlockLayer blockChainLastBlockLayer;

	@Autowired
	private TransferDao transferDao;

	@Bean
	public DataSource dataSource() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(prop.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(prop.getProperty("jdbc.url"));
		dataSource.setUsername(prop.getProperty("jdbc.username"));
		dataSource.setPassword(prop.getProperty("jdbc.password"));
		return dataSource;
	}

	@Bean(initMethod = "migrate")
	public Flyway flyway() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final Flyway flyway = new Flyway();
		flyway.setDataSource(this.dataSource());
		flyway.setLocations(prop.getProperty("flyway.locations"));
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(this.dataSource());

		// TODO: it would be nicer, no get only hibernate props and add them all at once using .addProperties(properties);
		localSessionFactoryBuilder.setProperty("hibernate.dialect", prop.getProperty("hibernate.dialect"));
		localSessionFactoryBuilder.setProperty("hibernate.show_sql", prop.getProperty("hibernate.show_sql"));
		localSessionFactoryBuilder.setProperty("hibernate.use_sql_comments", prop.getProperty("hibernate.use_sql_comments"));
		localSessionFactoryBuilder.setProperty("hibernate.jdbc.batch_size", prop.getProperty("hibernate.jdbc.batch_size"));

		localSessionFactoryBuilder.addAnnotatedClasses(Account.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Block.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Transfer.class);
		return localSessionFactoryBuilder.buildSessionFactory();
	}

	@Bean
	public BlockChain blockChain() {
		return new BlockChain(this.accountAnalyzer(), this.accountDao, this.blockChainLastBlockLayer, this.blockDao, this.foraging());
	}

	@Bean
	public Foraging foraging() {
		return new Foraging(this.accountAnalyzer(), this.blockDao, this.blockChainLastBlockLayer, this.transferDao);
	}

	@Bean
	public AccountAnalyzer accountAnalyzer() {
		return new AccountAnalyzer(new PoiAlphaImportanceGeneratorImpl());
	}

	@Bean BlockScorer blockScorer() {
		return new BlockScorer(this.accountAnalyzer());
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}

	@Bean
	public NisMain nisMain() {
		return new NisMain();
	}

	@Bean
	public NisPeerNetworkHost nisPeerNetworkHost() {
		return new NisPeerNetworkHost(this.accountAnalyzer(), this.blockChain());
	}

	@Bean
	public DeserializerHttpMessageConverter deserializerHttpMessageConverter() {
		return new DeserializerHttpMessageConverter(this.accountAnalyzer());
	}

	@Bean
	public SerializableEntityHttpMessageConverter serializableEntityHttpMessageConverter() {
		return new SerializableEntityHttpMessageConverter(this.deserializerHttpMessageConverter());
	}
}
