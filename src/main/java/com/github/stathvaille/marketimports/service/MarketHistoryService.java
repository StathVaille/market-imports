package com.github.stathvaille.marketimports.service;

import com.github.stathvaille.marketimports.domain.location.ImportLocation;
import com.github.stathvaille.marketimports.domain.staticdataexport.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * https://esi.tech.ccp.is/latest/markets/10000014/history/?datasource=tranquility&type_id=2185
 */
@Service
public class MarketHistoryService {

    private static final String apiTemplate = "https://esi.tech.ccp.is/latest/markets/%s/history/?datasource=tranquility&type_id=%s";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public Double getAverageNumberOfSalesInPast7Days(Item item, ImportLocation importLocation){
        logger.info("Calculating volume of " + item.getName() + " sold in the last 7 days in " + importLocation.getRegionName());
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(apiTemplate, importLocation.getRegionId(), item.getTypeId());
        Map[] itemHistoryArray = restTemplate.getForObject(url, HashMap[].class);
        List<Map> itemHistoryList = Arrays.asList(itemHistoryArray);


        LocalDate sevenDaysAgo = LocalDate.now().minusDays(8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map> last7ItemHistory = itemHistoryList.stream().filter(itemHistory ->  {
            LocalDate historicDate = LocalDate.parse((String)itemHistory.get("date"), formatter);
            return historicDate.isAfter(sevenDaysAgo);
        }).collect(Collectors.toList());

        List<Integer> last7ItemVolumes = last7ItemHistory.stream().map(itemHistory -> (int) itemHistory.get("volume")).collect(Collectors.toList());
        int totalSoldInLast7Days = last7ItemVolumes.stream().mapToInt(a -> a).sum();
        double averageSoldInLast7Days = totalSoldInLast7Days / 7.0;
        return averageSoldInLast7Days;
    }

    public Map<Item, Double> getAverageNumberOfSalesInPast7Days(Collection<Item> items, ImportLocation importLocation){
        Map<Item, Double> itemAverageSalesVolume = new HashMap<>();
        items.parallelStream().forEach(item ->
                itemAverageSalesVolume.put(item, getAverageNumberOfSalesInPast7Days(item, importLocation))
        );
        return itemAverageSalesVolume;
    }

}