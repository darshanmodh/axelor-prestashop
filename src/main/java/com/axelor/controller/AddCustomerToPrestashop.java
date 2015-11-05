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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.axelor.app.prestashop.db.PrestashopConfiguration;
import com.axelor.apps.base.db.Partner;
import com.axelor.pojo.Address;
import com.axelor.pojo.Customer;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;

public class AddCustomerToPrestashop {

	PrestashopConfiguration configure = PrestashopConfiguration.all()
			.fetchOne();
	String apiKey = configure.getApiKey();
	InputStream inputStream;
	OutputStream outputStream;

	@Transactional
	public void saveToPrestashop(ActionRequest request, ActionResponse response) {
		String prestashopDetails[] = new String[2];
		Customer pojoCustomer = new Customer();
		Address pojoAddress = new Address();
		 
		com.axelor.apps.base.db.Partner prestashopCustomer = request
				.getContext().asType(com.axelor.apps.base.db.Partner.class);

		pojoCustomer.setFirstname(prestashopCustomer.getFirstName());
		pojoCustomer.setLastname(prestashopCustomer.getName());
		pojoCustomer.setCompany(prestashopCustomer.getCompany());
		pojoCustomer.setPasswd(prestashopCustomer.getPassword());
		pojoCustomer.setEmail(prestashopCustomer.getEmail());
		pojoCustomer.setActive( prestashopCustomer.getActive().equals(Boolean.TRUE)  ? 1 : 0);
		try {
			pojoCustomer.setDeleted( prestashopCustomer.getArchived().equals(Boolean.TRUE) ? 1 : 0);
		} catch(NullPointerException e) {
			pojoCustomer.setDeleted(0);
		}
		
		pojoCustomer.setId_default_group(prestashopCustomer
				.getPrestashopCustomerGroup().getId_group());
		Date date = prestashopCustomer.getBirthdate().toDateMidnight().toDate();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String strDate = simpleDateFormat.format(date);
		pojoCustomer.setBirthday(strDate);
		pojoCustomer.setAssociations(prestashopCustomer.getPrestashopCustomerGroup().getId_group().toString());
		
		pojoAddress.setId_country((int)(long)prestashopCustomer.getPrestashopCustomerAddress().getAddressL7Country().getPrestashopCountryId());
		pojoAddress.setId_state((int)(long)prestashopCustomer.getPrestashopCustomerAddress().getRegion().getId());
		pojoAddress.setFirstname(prestashopCustomer.getFirstName());
		pojoAddress.setLastname(prestashopCustomer.getName());
		pojoAddress.setPhone_mobile((prestashopCustomer.getPrestashopCustomerAddress().getPhoneMobile()));
		pojoAddress.setAddress1(prestashopCustomer
				.getPrestashopCustomerAddress().getAddressL2()
				+ " "
				+ prestashopCustomer.getPrestashopCustomerAddress()
						.getAddressL3());
		pojoAddress.setAddress2(prestashopCustomer
				.getPrestashopCustomerAddress().getAddressL4()
				+ " "
				+ prestashopCustomer.getPrestashopCustomerAddress()
						.getAddressL5());
		pojoAddress.setCity(prestashopCustomer.getPrestashopCustomerAddress()
				.getAddressL6());
		pojoAddress.setPostcode(prestashopCustomer.getPrestashopCustomerAddress().getPostcode());
		pojoAddress.setAlias(prestashopCustomer.getFirstName() + " "
				+ prestashopCustomer.getName());
		
		try {
			// check for the action
			if (prestashopCustomer.getPrestashopid() == 0) {
				prestashopDetails = saveCustomer(pojoCustomer);
				
				prestashopCustomer.setPrestashopid(Integer
						.parseInt(prestashopDetails[0]));
				System.out.println("PRESTA ID :: "+prestashopCustomer.getPrestashopid());
				prestashopCustomer.setPassword(prestashopDetails[1]);				
				
				pojoAddress.setId_customer(prestashopCustomer.getPrestashopid());
				prestashopDetails[0] = saveAddress(pojoAddress);
				prestashopCustomer.getPrestashopCustomerAddress().setPrestashopid( Integer.parseInt(prestashopDetails[0]) );				
				
				prestashopCustomer.save();
				response.setValues(prestashopCustomer);
				
				if (prestashopCustomer.getPrestashopid() != 0)
					response.setFlash("Successfully Added.");
				else
					response.setFlash("Prestashop server Error.");
			} else {
				String message = "";
				int prestashopId = prestashopCustomer.getPrestashopid();
				message = updateCustomer(prestashopId, pojoCustomer);
				
				int prestashopAddressId = prestashopCustomer.getPrestashopCustomerAddress().getPrestashopid();
				System.out.println("PRESTASHOP ADDRESS ID ::: " + prestashopAddressId);
				//check for action
				if (prestashopAddressId == 0) {
					pojoAddress.setId_customer(prestashopId);
					prestashopDetails[0] = saveAddress(pojoAddress);
					prestashopCustomer.getPrestashopCustomerAddress().setPrestashopid( Integer.parseInt(prestashopDetails[0]) );				
				} 
				else {
					pojoAddress.setId_customer(prestashopId);
					message = updateAddress(prestashopAddressId, pojoAddress);
				}
				
				prestashopCustomer.save();
				response.setValues(prestashopCustomer);
				
				if (prestashopCustomer.getPrestashopid() != 0)
					response.setFlash(message);
				else
					response.setFlash("Prestashop server Error.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Transactional
	public String[] saveCustomer(Customer pojoCustomer) throws Exception {
		String prestashopDetails[] = new String[2];
		URL url = new URL(
				"http://localhost/client-lib/crud/action.php?resource=customers&action=create&Akey="
						+ apiKey);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		
		JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		outputStream = connection.getOutputStream();
		jaxbMarshaller.marshal(pojoCustomer, outputStream);
		connection.connect();
		inputStream = connection.getInputStream();
		Scanner scan = new Scanner(inputStream);
		while (scan.hasNext()) {
			String temp = scan.nextLine();
			System.out.println(temp);
			Pattern idPattern = Pattern.compile("^id: ([0-9]+).*");
			Matcher idMatcher = idPattern.matcher(temp);
			Pattern passwdPattern = Pattern
					.compile("^passwd: ([0-9a-zA-Z]+).*");
			Matcher passwdMatcher = passwdPattern.matcher(temp);

			if (idMatcher.find())
				prestashopDetails[0] = idMatcher.group(1);
			if (passwdMatcher.find())
				prestashopDetails[1] = passwdMatcher.group(1);
		}
		scan.close();
		return prestashopDetails;
	}

	@Transactional
	public String saveAddress(Address pojoAddress) throws Exception {
		String prestashopDetails = "";
		URL url = new URL(
				"http://localhost/client-lib/crud/action.php?resource=addresses&action=create&Akey="
						+ apiKey);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		JAXBContext jaxbContext1 = JAXBContext.newInstance(Address.class);
		Marshaller jaxbMarshaller = jaxbContext1.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		outputStream = connection.getOutputStream();
		jaxbMarshaller.marshal(pojoAddress, outputStream);
		connection.connect();

		inputStream = connection.getInputStream();
		Scanner scan = new Scanner(inputStream);
		while (scan.hasNext()) {
			String temp = scan.nextLine();
			Pattern idPattern = Pattern.compile("^id: ([0-9]+).*");
			Matcher idMatcher = idPattern.matcher(temp);
			if (idMatcher.find()) {
				prestashopDetails = idMatcher.group(1);
			}
		}
		scan.close();
		return prestashopDetails;
	}

	public String updateCustomer(int prestashopId, Customer pojoCustomer)
			throws Exception {
		URL url = new URL(
				"http://localhost/client-lib/crud/action.php?resource=customers&action=update&id="
						+ prestashopId + "&Akey=" + apiKey);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		connection.setDoOutput(true);
		connection.setDoInput(true);

		outputStream = connection.getOutputStream();
		jaxbMarshaller.marshal(pojoCustomer, outputStream);
		connection.connect();

		inputStream = connection.getInputStream();
		Scanner scan = new Scanner(inputStream);
		String message = "";
		while (scan.hasNext()) {
			message += scan.nextLine();
		}
		System.out.println(message);
		scan.close();
		return message;
	}

	public String updateAddress(int prestashopAddressId, Address pojoAddress)
			throws Exception {
		URL url = new URL(
				"http://localhost/client-lib/crud/action.php?resource=addresses&action=update&id="
						+ prestashopAddressId + "&Akey=" + apiKey);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		JAXBContext jaxbContext = JAXBContext.newInstance(Address.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		connection.setDoOutput(true);
		connection.setDoInput(true);

		outputStream = connection.getOutputStream();
		jaxbMarshaller.marshal(pojoAddress, outputStream);
		connection.connect();

		inputStream = connection.getInputStream();
		Scanner scan = new Scanner(inputStream);
		String message = "";
		while (scan.hasNext()) {
			message += scan.nextLine();
		}
		scan.close();
		return message;
	}

	@Transactional
	public void deleteCustomer(ActionRequest request, ActionResponse response)
			throws IOException {
		
		com.axelor.apps.base.db.Partner prestashopCustomer = request
				.getContext().asType(com.axelor.apps.base.db.Partner.class);
			Customer pojoCustomerForDelete = new Customer();
			try {
				pojoCustomerForDelete.setDeleted(1);
				String message = updateCustomer(prestashopCustomer.getPrestashopid(), pojoCustomerForDelete);
				prestashopCustomer.setArchived(Boolean.TRUE);
				prestashopCustomer.save();
				System.out.println(message);
				response.setFlash(prestashopCustomer.getFirstName() + " is successfully deleted.");
			}catch(Exception e)
			{
				e.printStackTrace();
			}
	}
}

