;; Ororo is a library for working with the wunderground API. The API is pretty simple and
;; not too large, so it can mostly be done automatically. Some pieces are a bit inconsistent
;; and are implemented differently.
(ns ororo.core
  (:require [ororo.async :as async]))

(def geolookup
  "Returns the city name, zip code/postal code, latitude-longitude coordinates and
   and nearby weather stations."
  (comp deref async/geolookup))

(def conditions
  "Returns the current temperature, weather condition, humidity, wind, 'feels like'
   temperature, barometric pressure, and visibility."
  (comp deref async/conditions))

(def forecast
  "Returns a summar of the weather for the next 3 days. This includes high and low
   temperatures, a string text forecast, and the conditions."
  (comp deref async/forecast))

(def astronomy "Returns the moon phase, sunrise, and sunset times."
  (comp deref async/astronomy))

(def radar "Returns links to radar images."
  (comp deref async/radar))

(def satellite "Returns links to visual and infrared satellite images."
  (comp deref async/satellite))

(def webcams
  "Returns locations of nearby Personal Weather Stations and URLS for
   images from their web cams."
  (comp deref async/webcams))

(def alerts
  "Returns the short name description, expiration time, and a long text description
   of a severe weather alert if one has been issued for the searched."
  (comp deref async/alerts))

(def hourly
  "Returns an hourly forecast for the next 36 hours immediately following
  the api request."
  (comp deref async/hourly))

(def yesterday "Returns a summary of the observed weather history for yesterday."
  (comp deref async/yesterday))

(def hourly-seven-day "Returns an hourly forecast for the next 7 days."
  (comp deref async/hourly-seven-day))

(def forecast-seven-day
  "Returns a summary of the weather for the next 7 days. This includes high and
   low temperatures, a string text forecast, and the conditions."
  (comp deref forecast-seven-day))

(def history
  "Returns a summary of the observed weather for the specified date, which
   Should be a string of the format YYYYMMDD."
  (comp deref async/history))
