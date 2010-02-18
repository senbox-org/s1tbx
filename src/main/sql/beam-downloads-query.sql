SELECT 
   from_unixtime(time_stamp), 
   bc_what, 
   bc_countryname, 
   bc_city, 
   bc_name, 
   bc_mail, 
   remote_host 
FROM 
   access_log  
where 
   NOT (remote_host like '192.168.%') AND 
   NOT (remote_host like '127.0.0.1') AND
   NOT (remote_host like '141.4.215.%')
