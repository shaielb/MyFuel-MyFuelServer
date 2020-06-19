package analytical;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import db.IDbComponent;
import db.entity.AnalyticalRatings;
import db.entity.AnalyticalView;
import db.entity.Customer;
import db.interfaces.IEntity;
import ioc.IIocContainer;
import messages.QueryContainer;

/**
 * @author shaielb
 *
 */
public class AnalyticalSystem implements Runnable {

	/**
	 * 
	 */
	private static final String DbComponent = "dbComponent";

	/**
	 * 
	 */
	private IIocContainer _iocContainer;

	/**
	 * 
	 */
	private IDbComponent _dbComponent;

	private List<QueryContainer> _avQueryContainers = new ArrayList<QueryContainer>();
	private QueryContainer _avQueryContainer = new QueryContainer();

	private List<QueryContainer> _arQueryContainers = new ArrayList<QueryContainer>();
	private QueryContainer _arQueryContainer = new QueryContainer();
	
	private Map<String, Map<String, Map<Time, Map<Time, Integer>>>> _ratingMap = 
			new HashMap<String, Map<String, Map<Time, Map<Time, Integer>>>>();

	/**
	 * @param iocContainer
	 * @throws Exception
	 */
	public AnalyticalSystem(IIocContainer iocContainer) throws Exception {
		_iocContainer = iocContainer;
		_dbComponent = (IDbComponent) _iocContainer.resolve(DbComponent);
		_avQueryContainers.add(_avQueryContainer);
		_arQueryContainers.add(_arQueryContainer);
		
	}

	/**
	 *
	 */
	@Override
	public void run() {
		try {
			prepareRatingsMap();
			
			Map<Integer, Integer> ratings = new HashMap<Integer, Integer>();

			AnalyticalView analyticalView = new AnalyticalView();
			_avQueryContainer.setQueryEntity(analyticalView);

			List<IEntity> avList = _dbComponent.filter(_avQueryContainers);
			for (IEntity avEntity : avList) {
				AnalyticalView av = (AnalyticalView) avEntity;
				Integer currRate = ratings.get(av.getCustomerId());
				if (currRate == null) {
					ratings.put(av.getCustomerId(), currRate = 0);
				}
				Map<Time, Map<Time, Integer>> startTimes = _ratingMap.get(av.getModelType()).get(av.getFuelType());
				for (Entry<Time, Map<Time, Integer>> startTimeEntry : startTimes.entrySet()) {
					if (av.getDateTime().getHours() >= startTimeEntry.getKey().getHours()) {
						Map<Time, Integer> endTime = startTimeEntry.getValue();
						for (Entry<Time, Integer> endTimeEntry : endTime.entrySet()) {
							if (av.getDateTime().getHours() <= endTimeEntry.getKey().getHours()) {
								ratings.put(av.getCustomerId(), currRate += endTimeEntry.getValue());
							}
						}
					}
				}
			}
			Customer customer = new Customer();
			for (Entry<Integer, Integer> entry : ratings.entrySet()) {
				customer.setId(entry.getKey());
				customer.setLastRatingTime(new Timestamp(System.currentTimeMillis()));
				Double result = (entry.getValue().doubleValue() / 45) * 10;
				customer.setRating((double) result.intValue());
				_dbComponent.update(customer);
			}
			Thread.sleep(604800000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// check the last update for each client
		// if the last check was more than a week ago - perform a check and update
		// if not go to sleep for (week - last update time)
	}
	
	// customer_pricing_model, fuel_type, start_fueling_time, end_fueling_time, rating
	// Map<String, Map<String, Map<Time, Map<Time, Integer>>>> _ratingMap
	private void prepareRatingsMap() throws Exception {
		AnalyticalRatings analyticalRatings = new AnalyticalRatings();
		_arQueryContainer.setQueryEntity(analyticalRatings);
		List<IEntity> arList = _dbComponent.filter(_arQueryContainers);
		
		for (IEntity entity : arList) {
			AnalyticalRatings ar = (AnalyticalRatings) entity;
			Map<String, Map<Time, Map<Time, Integer>>> fuelTypeMap = _ratingMap.get(ar.getCustomerPricingModel());
			if (fuelTypeMap == null) {
				_ratingMap.put(ar.getCustomerPricingModel(), fuelTypeMap = new HashMap<String, Map<Time, Map<Time, Integer>>>());
			}
			Map<Time, Map<Time, Integer>> startFuelingTime = fuelTypeMap.get(ar.getFuelType());
			if (startFuelingTime == null) {
				fuelTypeMap.put(ar.getFuelType(), startFuelingTime = new HashMap<Time, Map<Time, Integer>>());
			}
			Map<Time, Integer> endFuelingTime = startFuelingTime.get(ar.getStartFuelingTime());
			if (endFuelingTime == null) {
				startFuelingTime.put(ar.getStartFuelingTime(), endFuelingTime = new HashMap<Time, Integer>());
			}
			endFuelingTime.put(ar.getEndFuelingTime(), ar.getRating());
		}
	}
}
