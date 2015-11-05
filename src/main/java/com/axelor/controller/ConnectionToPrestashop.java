/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.controller;

import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.io.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.axelor.app.prestashop.db.PrestashopConfiguration;
import com.axelor.app.prestashop.db.PrestashopCustomerGroup;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Country;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Region;
import com.axelor.apps.base.db.UserInfo;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.pojo.Customer;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;

public class ConnectionToPrestashop {
	// PrestashopConfiguration config =
	// PrestashopConfiguration.all().fetchOne();
	// String apiKey = config.getApiKey();
	String apiKey = "";

	public void testConnection(ActionRequest request, ActionResponse response)
			throws Exception {
		String message;
		PrestashopConfiguration configure = request.getContext().asType(
				PrestashopConfiguration.class);
		if (getConnection(configure.getApiKey(), configure.getUserName())) {
			message = "Successfully Connected";
			response.setValue("tested", Boolean.TRUE);
		} else {
			message = "Wrong Authentication Key or Key has been disabled.";
			response.setValue("tested", Boolean.FALSE);
		}
		response.setFlash(message);
	}

	public boolean getConnection(String apiKey, String userName) {
		String message = "";
		try {
			URL url = new URL(
					"http://localhost/client-lib/connection.php?Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				message += scan.nextLine();
			}
			scan.close();
			System.out.println("MESSAGE :: " + message);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message == "" ? true : false;
	}

	public void changeTested(ActionRequest request, ActionResponse response) {
		PrestashopConfiguration configure = request.getContext().asType(
				PrestashopConfiguration.class);
		User currentUser = AuthUtils.getUser();
		UserInfo currentUserInfo = UserInfo.all()
				.filter("self.internalUser = ?1", currentUser).fetchOne();
		response.setValue("userName", currentUserInfo.getFullName());
		if (configure.getTested() == Boolean.TRUE) {
			response.setValue("tested", Boolean.FALSE);
		}
	}

	@Transactional
	public void syncWithPrestashop(ActionRequest request,
			ActionResponse response) {
		String message = "";
		User currentUser = AuthUtils.getUser();
		UserInfo currentUserInfo = UserInfo.all()
				.filter("self.internalUser = ?1", currentUser).fetchOne();
		PrestashopConfiguration config = PrestashopConfiguration.all()
				.filter("userName=?", currentUserInfo.getFullName()).fetchOne();
		apiKey = config.getApiKey();
		if (syncCurrency() == "done")
			if (syncCustomer() == "done")
				if (syncCustomerGroup() == "done")
					if (syncAddress() == "done")
						message = "Synchronization is completed.";
					else
						message = "Address cannot be synchronized, Please check the key.";
				else
					message = "CustomerGroup cannnot be synchronized, Please check the key.";
			else
				message = "Customer cannot be synchronized, Please check the key.";
		else
			message = "Currency cannot be synchronized. Please check the key.";
		response.setFlash(message);
	}

	@Transactional
	@SuppressWarnings("finally")
	public String syncCustomer() {
		String message = "";
		try {
			List<Integer> prestashopIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<Partner> erpList = Partner.all().fetch();

			for (Partner prestahopCustomer : erpList) {
				erpIdList.add(prestahopCustomer.getPrestashopid());
			}
			System.out.println("API KEY :: " + apiKey);
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=customers&action=getallid&Akey="
							+ apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				String data = scan.nextLine();
				System.out.println(data);
				prestashopIdList.add(Integer.parseInt(data));
			}
			System.out.println("From Prestashop :: " + prestashopIdList.size());
			System.out.println("From ERP :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestshop
			Iterator<Integer> prestaListIterator = prestashopIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out
						.println("Current prestaid for operation ::" + tempId);
				if (erpIdList.contains(tempId)) {
					Customer tempCustomer = getCustomer(tempId);
					String dateUpdate = tempCustomer.getDate_upd();
					LocalDateTime dt1 = LocalDateTime.parse(dateUpdate,
							DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
					Partner pCust = Partner.all()
							.filter("prestashopId=?", tempId).fetchOne();
					LocalDateTime dt2 = pCust.getUpdatedOn();
					if (dt2 != null) {
						int diff = Seconds.secondsBetween(dt2, dt1)
								.getSeconds();
						if (diff > 1)
							updateCustomer(tempCustomer, tempId);
					} else {
						updateCustomer(tempCustomer, tempId);
					}
					erpIdList.remove(tempId);
				} else {
					System.out
							.println("Current prestaid for insertion operation ::"
									+ tempId);
					// insert new data in ERP Database
					insertCustomer(tempId);
					erpIdList.remove(tempId);
				}
			}
			if (erpIdList.isEmpty()) {
				System.out.println("Synchronization is completed.");
				message = "done";
			} else {
				// delete from ERP
				Iterator<Integer> erpListIterator = erpIdList.iterator();
				while (erpListIterator.hasNext()) {
					Integer tempId = erpListIterator.next();
					if (tempId != 0) {
						System.out.println("Currently in  Erp ::" + tempId);
						Partner customerDelete = Partner.all()
								.filter("prestashopid=?", tempId).fetchOne();
						String firstName = customerDelete.getFirstName();
						customerDelete.setArchived(Boolean.TRUE);
						System.out.println("customer deleted ::" + firstName);
					}
				}
				while (prestaListIterator.hasNext()) {
					Integer tempId = prestaListIterator.next();
					System.out.println("Currently in prestashop ::" + tempId);
				}
				System.out.println("Synchronization is completed.");
				message = "done";
			}
		} catch (Exception e) {
			message = "Wrong Authentication Key or Key has been disabled.";
		} finally {
			return message;
		}
	}

	@Transactional
	public Customer getCustomer(int customerId) {
		com.axelor.pojo.Customer pojoCustomer = new com.axelor.pojo.Customer();
		pojoCustomer.setFirstname("FIRSTNAME");
		pojoCustomer.setLastname("LASTNAME");
		pojoCustomer.setCompany("COMPANY");
		pojoCustomer.setPasswd("PASSWORD");
		pojoCustomer.setEmail("EMAIL");
		pojoCustomer.setBirthday("1000-01-01");
		pojoCustomer.setActive(0);
		pojoCustomer.setDeleted(0);
		pojoCustomer.setId_default_group(0);
		pojoCustomer.setAssociations("associations");
		pojoCustomer.setDate_upd("dateUpd");
		try {
			System.out.println("API KEY :: " + apiKey);
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=customers&action=retrieve&id="
							+ customerId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Customer.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoCustomer, outputStream);

			connection.connect();
			InputStream inputStream = connection.getInputStream();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoCustomer = (com.axelor.pojo.Customer) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pojoCustomer;
	}

	@Transactional
	public void updateCustomer(Customer pojoCustomer, int prestashopId) {
		DateTimeFormatter dateTimeFormat = DateTimeFormat
				.forPattern("yyyy-MM-dd");
		LocalDate dateOfBirth = dateTimeFormat.parseLocalDate(pojoCustomer
				.getBirthday());
		Partner prestashopCustomer = Partner.filter("prestashopid=?",
				prestashopId).fetchOne();
		prestashopCustomer.setPrestashopid(prestashopId);
		prestashopCustomer.setFirstName(pojoCustomer.getFirstname());
		prestashopCustomer.setName(pojoCustomer.getLastname());
		prestashopCustomer.setFullName(pojoCustomer.getLastname() + " "
				+ pojoCustomer.getFirstname());
		prestashopCustomer.setEmail(pojoCustomer.getEmail());
		prestashopCustomer.setCompany(pojoCustomer.getCompany());
		prestashopCustomer.setPassword(pojoCustomer.getPasswd());
		prestashopCustomer.setBirthdate(dateOfBirth);
		prestashopCustomer.setActive(BooleanUtils.toBoolean(pojoCustomer
				.getActive()));
		prestashopCustomer.setArchived(BooleanUtils.toBoolean(pojoCustomer
				.getDeleted()));
		System.out.println(pojoCustomer.getId_default_group());
		prestashopCustomer.setPrestashopCustomerGroup(PrestashopCustomerGroup
				.all().filter("id_group=?", pojoCustomer.getId_default_group())
				.fetchOne());
		System.out.println(prestashopCustomer.getPrestashopCustomerGroup()
				.getId_group());
		System.out.println("ASSOCIATIONS : " + pojoCustomer.getAssociations());
		prestashopCustomer.save();
	}

	@SuppressWarnings("null")
	@Transactional
	public void insertCustomer(int customerId) {
		com.axelor.pojo.Customer pojoCustomer = getCustomer(customerId);
		Partner prestashopCustomer = new Partner();
		DateTimeFormatter dateTimeFormat = DateTimeFormat
				.forPattern("yyyy-MM-dd");
		LocalDate dateOfBirth = dateTimeFormat.parseLocalDate(pojoCustomer
				.getBirthday());
		// add this customer into ERP
		prestashopCustomer.setPrestashopid(customerId);
		prestashopCustomer.setFirstName(pojoCustomer.getFirstname());
		prestashopCustomer.setName(pojoCustomer.getLastname());
		prestashopCustomer.setFullName(pojoCustomer.getLastname() + " "
				+ pojoCustomer.getFirstname());
		prestashopCustomer.setEmail(pojoCustomer.getEmail());
		prestashopCustomer.setCompany(pojoCustomer.getCompany());
		prestashopCustomer.setPassword(pojoCustomer.getPasswd());
		prestashopCustomer.setBirthdate(dateOfBirth);
		prestashopCustomer.setActive(BooleanUtils.toBoolean(pojoCustomer
				.getActive()));
		prestashopCustomer.setArchived(BooleanUtils.toBoolean(pojoCustomer
				.getDeleted()));
		System.out.println(pojoCustomer.getId_default_group());
		prestashopCustomer.setPrestashopCustomerGroup(PrestashopCustomerGroup
				.all().filter("id_group=?", pojoCustomer.getId_default_group())
				.fetchOne());
		System.out.println(prestashopCustomer.getPrestashopCustomerGroup()
				.getId_group());
		System.out.println("ASSOCIATIONS : " + pojoCustomer.getAssociations());
		prestashopCustomer.save();
	}

	@SuppressWarnings("finally")
	@Transactional
	public String syncCurrency() {
		String message = "";
		try {
			List<Integer> currencyIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<Currency> erpList = Currency.all().fetch();

			for (Currency erpCurrency : erpList) {
				erpIdList.add(erpCurrency.getPrestashopCurrencyId());
			}

			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=currencies&action=getallid&Akey="
							+ apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				String data = scan.nextLine();
				System.out.println(data);
				currencyIdList.add(Integer.parseInt(data));
			}
			System.out.println("From Prestashop :: " + currencyIdList.size());
			System.out.println("From ERP :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestahop
			Iterator<Integer> prestaListIterator = currencyIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out
						.println("Current prestaid for operation ::" + tempId);
				if (erpIdList.contains(tempId)) {
					erpIdList.remove(tempId);
				} else {
					System.out
							.println("Current prestaid for insertion operation ::"
									+ tempId);
					// insert new data in ERP Database
					insertCurrency(tempId);
					erpIdList.remove(tempId);
				}
			}
			if (erpIdList.isEmpty()) {
				System.out.println("Synchronization is completed.");
				message = "done";
			} else {
				// delete from ERP
				Iterator<Integer> erpListIterator = erpIdList.iterator();
				while (erpListIterator.hasNext()) {
					Integer tempId = erpListIterator.next();					
					if (tempId != 0) {
						System.out.println("Currently in  Erp ::" + tempId);						
						Currency currencyDelete = Currency.all()
								.filter("prestashopCurrencyId=?", tempId)
								.fetchOne();
						String currencyName = currencyDelete.getName();
						// customerGroupDelete.remove();
						currencyDelete.setArchived(Boolean.TRUE);
						System.out
								.println("customer deleted ::" + currencyName);
					}
				}
				while (prestaListIterator.hasNext()) {
					Integer tempId = prestaListIterator.next();
					System.out.println("Currently in prestashop ::" + tempId);
				}
				System.out.println("Synchronization is completed.");
				message = "done";
			}
		} catch (Exception e) {
			message = "Wrong Authentication Key or Key has been disabled.";
		} finally {
			return message;
		}
	}

	@SuppressWarnings("null")
	@Transactional
	public void insertCurrency(int currencyId) {
		com.axelor.pojo.Currency pojoCurrency = new com.axelor.pojo.Currency();
		pojoCurrency.setName("NAME");
		pojoCurrency.setIso_code("ISO_CODE");
		pojoCurrency.setSign("SIGN");
		try {
			System.out.println("Cuurency Id :: " + currencyId);
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=currencies&action=retrieve&id="
							+ currencyId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Currency.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoCurrency, outputStream);

			connection.connect();
			//InputStream inputStream = connection.getInputStream();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoCurrency = (com.axelor.pojo.Currency) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));
			// jaxbMarshaller.marshal(pojoCurrency, f);
			Currency checkCurrency = Currency.all()
					.filter("code=?", pojoCurrency.getIso_code()).fetchOne();
			if (checkCurrency != null) {
				checkCurrency.setPrestashopCurrencyId(currencyId);
			} else {
				Currency currency = new Currency();
				// add this currency into ERP
				currency.setName(pojoCurrency.getName());
				currency.setCode(pojoCurrency.getIso_code());
				currency.setSymbol(pojoCurrency.getSign());
				currency.setPrestashopCurrencyId(currencyId);
				currency.save();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@SuppressWarnings("finally")
	@Transactional
	public String syncCustomerGroup() {
		String message = "";
		try {
			List<Integer> groupIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<PrestashopCustomerGroup> erpList = PrestashopCustomerGroup
					.all().fetch();

			for (PrestashopCustomerGroup prestahopCustomerGroup : erpList) {
				erpIdList.add(prestahopCustomerGroup.getId_group());
			}

			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=groups&action=getallid&Akey="
							+ apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				String data = scan.nextLine();
				System.out.println(data);
				groupIdList.add(Integer.parseInt(data));
			}
			System.out.println("From Prestashop :: " + groupIdList.size());
			System.out.println("From ERP :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestahop
			Iterator<Integer> prestaListIterator = groupIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out
						.println("Current prestaid for operation ::" + tempId);
				if (erpIdList.contains(tempId)) {
					erpIdList.remove(tempId);
				} else {
					System.out
							.println("Current prestaid for insertion operation ::"
									+ tempId);
					// insert new data in ERP Database
					insertGroup(tempId);
					erpIdList.remove(tempId);
				}
			}
			if (erpIdList.isEmpty()) {
				System.out.println("Synchronization is completed.");
				message = "done";
			} else {
				// delete from ERP
				Iterator<Integer> erpListIterator = erpIdList.iterator();
				while (erpListIterator.hasNext()) {
					Integer tempId = erpListIterator.next();
					System.out.println("Currently in  Erp ::" + tempId);
					if (tempId != 0) {
						PrestashopCustomerGroup customerGroupDelete = PrestashopCustomerGroup
								.all().filter("id_group=?", tempId).fetchOne();
						String groupName = customerGroupDelete.getName();
						// customerGroupDelete.remove();
						customerGroupDelete.setArchived(Boolean.TRUE);
						System.out.println("customer deleted ::" + groupName);
					}
				}
				while (prestaListIterator.hasNext()) {
					Integer tempId = prestaListIterator.next();
					System.out.println("Currently in prestashop ::" + tempId);
				}
				System.out.println("Synchronization is completed.");
				message = "done";
			}
		} catch (Exception e) {
			message = "Wrong Authentication Key or Key has been disabled.";
		} finally {
			return message;
		}
	}

	@SuppressWarnings("null")
	@Transactional
	public void insertGroup(int groupId) {
		com.axelor.pojo.CustomerGroup pojoCustomerGroup = new com.axelor.pojo.CustomerGroup();
		pojoCustomerGroup.setId(0);
		pojoCustomerGroup.setLanguage("NAME");
		pojoCustomerGroup.setDescription("DESCRIPTION");
		try {
			System.out.println("GROUPID :: " + groupId);
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=groups&action=retrieve&id="
							+ groupId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			File f = new File("//home//axelor//Desktop//example//k.xml");
			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.CustomerGroup.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoCustomerGroup, outputStream);

			connection.connect();
			InputStream inputStream = connection.getInputStream();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoCustomerGroup = (com.axelor.pojo.CustomerGroup) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));
			jaxbMarshaller.marshal(pojoCustomerGroup, f);
			PrestashopCustomerGroup prestashopCustomerGroup = new PrestashopCustomerGroup();
			// add this customer into ERP
			prestashopCustomerGroup.setId_group(groupId);
			prestashopCustomerGroup.setName(pojoCustomerGroup.getLanguage());
			prestashopCustomerGroup.setDescription(pojoCustomerGroup
					.getDescription());
			prestashopCustomerGroup.save();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@SuppressWarnings("finally")
	@Transactional
	public String syncAddress() {
		String message = "";
		try {
			List<Integer> prestashopIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<Address> erpList = Address.all().fetch();

			for (Address prestahopAddress : erpList) {
				erpIdList.add(prestahopAddress.getPrestashopid());
			}

			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=addresses&action=getallid&Akey="
							+ apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				String data = scan.nextLine();
				System.out.println(data);
				prestashopIdList.add(Integer.parseInt(data));
			}
			System.out.println("From Prestashop Addresses :: "
					+ prestashopIdList.size());
			System.out.println("From ERP Addresses :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestashop
			Iterator<Integer> prestaListIterator = prestashopIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out
						.println("Current AddressPrestashopId for operation ::"
								+ tempId);
				if (erpIdList.contains(tempId)) {
					com.axelor.pojo.Address tempAddress = getAddress(tempId);
					String dateUpdate = tempAddress.getDate_upd();
					LocalDateTime dt1 = LocalDateTime.parse(dateUpdate,
							DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
					Address pAddress = Address.all()
							.filter("prestashopid=?", tempId).fetchOne();
					LocalDateTime dt2 = pAddress.getUpdatedOn();
					if (dt2 != null) {
						int diff = Seconds.secondsBetween(dt2, dt1)
								.getSeconds();
						if (diff > 1)
							updateAddress(tempAddress, tempId);
					} else {
						updateAddress(tempAddress, tempId);
					}
					erpIdList.remove(tempId);
				} else {
					System.out
							.println("Current AddressPrestashopId for insertion operation ::"
									+ tempId);
					// insert new data in ERP Database
					insertAddress(tempId);
					erpIdList.remove(tempId);
				}
			}
			if (erpIdList.isEmpty()) {
				System.out.println("Synchronization is completed.");
				message = "done";
			} else {
				// delete from ERP
				Iterator<Integer> erpListIterator = erpIdList.iterator();
				while (erpListIterator.hasNext()) {
					Integer tempId = erpListIterator.next();
					if (tempId != 0) {
						Address addressDelete = Address.all()
								.filter("prestashopid=?", tempId).fetchOne();
						String fullName = addressDelete.getFullName();
						// addressDelete.remove();
						addressDelete.setArchived(Boolean.TRUE);
						System.out.println("Address deleted ::" + fullName);
					}
				}
				while (prestaListIterator.hasNext()) {
					Integer tempId = prestaListIterator.next();
					System.out.println("Currently in prestashop ::" + tempId);
				}
				System.out.println("Synchronization is completed.");
				message = "done";
			}
		} catch (Exception e) {
			message = "Wrong Authentication Key or Key has been disabled.";
		} finally {
			return message;
		}
	}

	@Transactional
	public com.axelor.pojo.Address getAddress(int addressId) {
		com.axelor.pojo.Address pojoAddress = new com.axelor.pojo.Address();
		pojoAddress.setId_customer(0);
		pojoAddress.setId_country(0);
		pojoAddress.setId_state(0);
		pojoAddress.setPrestashopAddressId(0);
		pojoAddress.setFirstname("FIRSTNAME");
		pojoAddress.setLastname("LASTNAME");
		pojoAddress.setPhone_mobile("0000000000");
		pojoAddress.setAddress1("ADDRESS1");
		pojoAddress.setAddress2("ADDRESS2");
		pojoAddress.setCity("CITY");
		pojoAddress.setPostcode("000000");
		pojoAddress.setAlias("ALIAS");
		pojoAddress.setDate_upd("dateUpd");
		try {
			System.out.println("API KEY :: " + apiKey);
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=addresses&action=retrieve&id="
							+ addressId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Address.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoAddress, outputStream);

			connection.connect();
			InputStream inputStream = connection.getInputStream();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoAddress = (com.axelor.pojo.Address) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pojoAddress;
	}

	@Transactional
	public void updateAddress(com.axelor.pojo.Address pojoAddress,
			int prestashopId) {
		Address prestashopAddress = Address.filter("prestashopid=?",
				prestashopId).fetchOne();
		// Resolve Country
		Country baseCountry = Country.all()
				.filter("prestashop_country_id=?", pojoAddress.getId_country())
				.fetchOne();
		// Resolve Region
		Region baseRegion = Region.all()
				.filter("prestashop_region_id=?", pojoAddress.getId_state())
				.fetchOne();
		// add this address into ERP
		prestashopAddress.setAddressL2(pojoAddress.getFirstname());
		prestashopAddress.setAddressL3(pojoAddress.getLastname());
		prestashopAddress.setAddressL4(pojoAddress.getAddress1());
		prestashopAddress.setAddressL5(pojoAddress.getAddress2());
		prestashopAddress.setAddressL6(pojoAddress.getPostcode() + " "
				+ pojoAddress.getCity());
		prestashopAddress.setFullName(pojoAddress.getAlias());
		prestashopAddress.setAddressL7Country(baseCountry);
		prestashopAddress.setPhoneMobile(pojoAddress.getPhone_mobile());
		prestashopAddress.setPostcode(pojoAddress.getPostcode());
		prestashopAddress.setPrestashopid(prestashopId);
		prestashopAddress.setRegion(baseRegion);
		prestashopAddress.save();
	}

	@SuppressWarnings("null")
	@Transactional
	public void insertAddress(int addressId) {
		com.axelor.pojo.Address pojoAddress = new com.axelor.pojo.Address();
		pojoAddress.setId_customer(0);
		pojoAddress.setId_country(0);
		pojoAddress.setId_state(0);
		pojoAddress.setPrestashopAddressId(0);
		pojoAddress.setFirstname("FIRSTNAME");
		pojoAddress.setLastname("LASTNAME");
		pojoAddress.setPhone_mobile("0000000000");
		pojoAddress.setAddress1("ADDRESS1");
		pojoAddress.setAddress2("ADDRESS2");
		pojoAddress.setCity("CITY");
		pojoAddress.setPostcode("000000");
		pojoAddress.setAlias("ALIAS");
		try {
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=addresses&action=retrieve&id="
							+ addressId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Address.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoAddress, outputStream);

			connection.connect();
			InputStream inputStream = connection.getInputStream();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoAddress = (com.axelor.pojo.Address) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));

			// Resolve Country
			Country baseCountry = Country
					.all()
					.filter("prestashop_country_id=?",
							pojoAddress.getId_country()).fetchOne();

			// Resolve Region
			Region baseRegion = Region
					.all()
					.filter("prestashop_region_id=?", pojoAddress.getId_state())
					.fetchOne();

			Address prestashopAddress = new Address();
			// add this address into ERP
			prestashopAddress.setAddressL2(pojoAddress.getFirstname());
			prestashopAddress.setAddressL3(pojoAddress.getLastname());
			prestashopAddress.setAddressL4(pojoAddress.getAddress1());
			prestashopAddress.setAddressL5(pojoAddress.getAddress2());
			prestashopAddress.setAddressL6(pojoAddress.getPostcode() + " "
					+ pojoAddress.getCity());
			prestashopAddress.setFullName(pojoAddress.getAlias());
			prestashopAddress.setAddressL7Country(baseCountry);
			prestashopAddress.setPhoneMobile(pojoAddress.getPhone_mobile());
			prestashopAddress.setPostcode(pojoAddress.getPostcode());
			prestashopAddress.setPrestashopid(addressId);
			prestashopAddress.setRegion(baseRegion);

			prestashopAddress.save();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
