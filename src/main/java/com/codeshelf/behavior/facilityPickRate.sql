with age_bin as (
     select extract(epoch from age(created, CAST(:endDateTime AS TIMESTAMP))) as age,
		        width_bucket(extract(epoch from age(created, CAST(:endDateTime AS TIMESTAMP))),
		                     EXTRACT(EPOCH FROM (CAST(:startDateTime AS TIMESTAMP) - CAST(:endDateTime AS TIMESTAMP))),
		                     0,
		                     :numBins) as bin
		   from event_worker
		  where parent_persistentid = :facilityId
		    and event_type = 'COMPLETE'
		    and created between CAST(:startDateTime AS TIMESTAMP) and CAST(:endDateTime AS TIMESTAMP)
   order by age asc
)

   select series, count(age_bin.bin)
     from generate_series(1, :numBins) as series
left join age_bin
	     on series.series = age_bin.bin
 group by series.series
 order by series.series;
