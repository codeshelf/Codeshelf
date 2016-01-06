-- note the java String.format WHERE CLAUSE where parameter that needs to get replaced
select bin, count(bin) as value from (
  select CAST(floor(extract(epoch from (created - CAST(:startDateTime AS TIMESTAMP)))  / :binWidth) AS integer) as bin
    from event_worker
   where %s
) AS event_bin
GROUP BY event_bin.bin
ORDER BY event_bin.bin
