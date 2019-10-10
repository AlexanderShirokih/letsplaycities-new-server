package ru.quandastudio.lpsserver.data.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseDAO {

	public interface InTransaction {
		/***
		 * 
		 * @param session current Session
		 */
		public Object execute(Session session);
	}

	@Autowired
	protected SessionFactory sessionFactory;

	/***
	 * 
	 * @param transactionalRequest
	 * @param commit               {@code true} if commit is needed, {@code false}
	 *                             otherwise.
	 * @return result of transactionalRequest
	 */
	protected Object transactional(InTransaction transactionalRequest) {
		final Session session = sessionFactory.getCurrentSession();
		final Transaction t = session.beginTransaction();
		final Object result = transactionalRequest.execute(session);
		t.commit();
		return result;
	}

}
