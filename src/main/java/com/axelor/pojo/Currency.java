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
package com.axelor.pojo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement(name="currency")
public class Currency {
	int prestashopCurrencyId;
	String name;
	String iso_code;
	String sign;

	public int getPrestashopCurrencyId() {
		return prestashopCurrencyId;
	}
	@XmlElement
	public void setPrestashopCurrencyId(int prestashopCurrencyId) {
		this.prestashopCurrencyId = prestashopCurrencyId;
	}

	public String getName() {
		return name;
	}
	@XmlElement
	public void setName(String name) {
		this.name = name;
	}

	public String getIso_code() {
		return iso_code;
	}
	@XmlElement
	public void setIso_code(String iso_code) {
		this.iso_code = iso_code;
	}

	public String getSign() {
		return sign;
	}
	@XmlElement
	public void setSign(String sign) {
		this.sign = sign;
	}
}
