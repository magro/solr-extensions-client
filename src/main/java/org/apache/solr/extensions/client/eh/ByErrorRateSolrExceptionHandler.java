package org.apache.solr.extensions.client.eh;

import java.util.Collection;
import java.util.Date;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Disables Solrs by the error rate they receive.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class ByErrorRateSolrExceptionHandler implements SolrExceptionHandler {

   private final Logger logger = LoggerFactory.getLogger(getClass());

   // this is the maximum errors per minute. If a zombie canidate bypasses this
   // limit, it's kicked onto the graveyard in the super class.
   private int maximumErrorsPerServerPerMinute = 10;

   // if these exceptions occur, take the server offline immediately.
   private final Collection<String> immediateDeadExcetionClassnames = Sets.newHashSet();

   /**
    * Adds an exception to the immediate death list.
    */
   public void addImmediateDeadException(Class<?> exception) {
      Preconditions.checkNotNull(exception);

      immediateDeadExcetionClassnames.add(exception.getCanonicalName());
   }

   /**
    * Sets the maximum errors per server per minute rate.
    */
   public void setMaximumErrorsPerServerPerMinute(int maximumErrorsPerServerPerMinute) {
      Preconditions.checkArgument(maximumErrorsPerServerPerMinute > 0);

      this.maximumErrorsPerServerPerMinute = maximumErrorsPerServerPerMinute;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void exceptionRaised(SolrRequest request, SolrServerWrapper wrapper, Exception e) {
      Preconditions.checkNotNull(wrapper);
      Preconditions.checkNotNull(e);

      // increase error count on the server
      wrapper.getExceptions().put(new Date(), e);

      // immediately death
      if (immediateDeadExcetionClassnames.contains(e.getClass().getCanonicalName())) {
         logger.info("Caught exception {} causing immediate death of {} ...", e.getClass().getCanonicalName(), wrapper);
         wrapper.setState(SolrServerState.DEAD);
      } else {
         // check error rate
         int epm = Maps.filterKeys(wrapper.getExceptions(), currentMinute()).size();

         if (epm > maximumErrorsPerServerPerMinute) {
            logger.info("Caught exception {} that rose error rate for {} to {} (above threshold of {} epm). Disabling server ...", e
                  .getClass().getCanonicalName(), wrapper, epm, maximumErrorsPerServerPerMinute);
            wrapper.setState(SolrServerState.DEAD);
         }
      }
   }

   public static Predicate<Date> currentMinute() {
      return new Predicate<Date>() {
         @Override
         public boolean apply(Date input) {
            long aMinuteAgo = System.currentTimeMillis() - (1000 * 60);
            return input.getTime() >= aMinuteAgo;
         }
      };
   }
}
