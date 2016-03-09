package org.springframework.ldap.samples.plain.dao;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.samples.plain.domain.Person;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import java.util.List;
import static org.springframework.ldap.query.LdapQueryBuilder.query;

public class PersonDaoImpl implements PersonDao {
	private LdapTemplate ldapTemplate;

	@Override
	public void create(Person person) {
		Name dn = buildDn(person);
		DirContextAdapter context = new DirContextAdapter(dn);
		mapToContext(person, context);
		ldapTemplate.bind(dn, context, null);
	}

	@Override
	public void update(Person person) {
		Name dn = buildDn(person);
		DirContextAdapter context = (DirContextAdapter) ldapTemplate.lookup(dn);
		mapToContext(person, context);
		ldapTemplate.modifyAttributes(dn, context.getModificationItems());
	}

	@Override
	public void delete(Person person) {
		ldapTemplate.unbind(buildDn(person));
	}

	@Override
	public List<String> getAllPersonNames() {
		return ldapTemplate.search(query().attributes("cn")
				.where("objectclass").is("person"),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs)
							throws NamingException {
						return attrs.get("cn").get().toString();
					}
				});
	}

	@Override
	public List<Person> findAll() {
		return ldapTemplate.search(query().where("objectclass").is("person"),
				PERSON_CONTEXT_MAPPER);
	}

	@Override
	public Person findByPrimaryKey(String country, String company,
			String fullname) {
		LdapName dn = buildDn(country, company, fullname);
		return ldapTemplate.lookup(dn, PERSON_CONTEXT_MAPPER);
	}

	private LdapName buildDn(Person person) {
		return buildDn(person.getCountry(), person.getCompany(),
				person.getFullName());
	}

	private LdapName buildDn(String country, String company, String fullname) {
		return LdapNameBuilder.newInstance().add("c", country)
				.add("ou", company).add("cn", fullname).build();
	}

	private void mapToContext(Person person, DirContextAdapter context) {
		context.setAttributeValues("objectclass", new String[] { "top",
				"person" });
		context.setAttributeValue("cn", person.getFullName());
		context.setAttributeValue("sn", person.getLastName());
		context.setAttributeValue("description", person.getDescription());
		context.setAttributeValue("telephoneNumber", person.getPhone());
	}

	/**
	 * Maps from DirContextAdapter to Person objects. A DN for a person will be
	 * of the form <code>cn=[fullname],ou=[company],c=[country]</code>, so the
	 * values of these attributes must be extracted from the DN. For this, we
	 * use the LdapName along with utility methods in LdapUtils.
	 */
	private final static ContextMapper<Person> PERSON_CONTEXT_MAPPER = new AbstractContextMapper<Person>() {
		@Override
		public Person doMapFromContext(DirContextOperations context) {
			Person person = new Person();
			LdapName dn = LdapUtils.newLdapName(context.getDn());
			person.setCountry(LdapUtils.getStringValue(dn, 0));
			person.setCompany(LdapUtils.getStringValue(dn, 1));
			person.setFullName(context.getStringAttribute("cn"));
			person.setLastName(context.getStringAttribute("sn"));
			person.setDescription(context.getStringAttribute("description"));
			person.setPhone(context.getStringAttribute("telephoneNumber"));
			return person;
		}
	};

	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}
}