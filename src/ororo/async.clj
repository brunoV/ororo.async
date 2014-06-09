;; Ororo is a library for working with the wunderground API. The API is pretty simple and
;; not too large, so it can mostly be done automatically. Some pieces are a bit inconsistent
;; and are implemented differently.
(ns ororo.async
  (:use [clojure.string :only [join]])
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(def base-url "http://api.wunderground.com/api/")

(defn- create-url
  "Intelligently create a URL out of an API key, features (api calls), and
   the query location."
  [key features location]
  (str base-url key "/"
       (if (coll? features)
         (join "/" (map name features))
         (name features))
       "/q/"
       (.replace
        (if (string? location)
          ^String location
          (join "/" (reverse location)))
        " "
        "_")
       ".json"))

(defn- sift [m f]
  (if f
    (or (f m) (-> m :response :results))
    m))

(defn- read-json [body]
  "Parse JSON from a string. Returns nil if argument is nil."
  ;; :key-fn keyword converts map keys into keywords
  (and body (json/read-str body :key-fn clojure.core/keyword)))

(defn- parse [sift-fn resp]
  "Parse a response map to an ororo response using the provided sift function."
  (-> resp
      :body
      read-json
      (sift sift-fn)))

;; Ororo provides a lot of functions for working with the wunderground API by default.
;; However, wunderground allows you to get more than one type of data in a single request.
;; This cuts down on API requests one has to make to get a lot of information. For those
;; situations, you can use this function to execute a request yourself.
(defn api-call
  "Make an API call out of a key, some features (as keywords, or just one keyword),
   a location, and a 'sifting' function that will be applied to the resulting map.
   Optionally accepts an options map that will be forwarded to httpkit's get.
   Returns a promise. "
  [key features location sift-fn & [opts f]]
  (let [req-opts (merge {:headers {" Accept-Encoding " " "}} opts)
        req (-> (create-url key features location)
                (http/get req-opts (when f (comp f (partial parse sift-fn)))))]
    (->> req
        deref
        (parse sift-fn)
        delay)))

;; Despite the original author's wishes, we are going to go with a macro to
;; generate all the API endpoints.

;; This is so that we can provide an API whose signatures mirror httpkit's request
;; methods, with two variadic overloaded versions: one that optionally takes a
;; callback function and another one that takes an options map and a callback.
;; Since it's not really possible in Clojure to have two overloaded versions
;; of the same method, we dynamically introspect the type of the first argument
;; and do the appropriate dispatch to api-call. This is what httpkit's request
;; does in its macro too.

(defmacro ^:private defapi
  " Installs a function that makes an API call based on a bit of info. "
  [method doc feature sift-fn]
  `(defn ~method
     ~doc
     ~'{:arglists '([key location & [opts callback]] [key location & [callback]])}
     ~'[key location & [s1 s2]]
     (if (or (fn? ~'s1) (instance? clojure.lang.MultiFn ~'s1))
       (api-call ~'key ~feature ~'location ~sift-fn nil ~'s1)
       (api-call ~'key ~feature ~'location ~sift-fn ~'s1 ~'s2))))

(defapi geolookup
  " Returns the city name, zip code/postal code, latitude-longitude coordinates
        and and nearby weather stations. "
  :geolookup :location)

(defapi conditions
  " Returns a summary of the weather for the next 3 days. This includes high and low temperatures,
        a string text forecast, and the conditions. "
  :conditions :current_observation)

(defapi forecast
   " Returns a summar of the weather for the next 3 days. This includes high and low
        temperatures, a string text forecast, and the conditions. "
   :forecast :forecast)

(defapi astronomy " Returns the moon phase, sunrise, and sunset times. "
   :astronomy :moon_phase)

(defapi radar " Returns links to radar images. "
  :radar :radar)

(defapi satellite " Returns links to visual and infrared satellite images. "
  :satellite :satellite)

(defapi webcams
  " Returns locations of nearby Personal Weather Stations and URLS for
        images from their web cams. "
  :webcams :webcams)

(defapi alerts
  " Returns the short name description, expiration time, and a long text description
        of a severe weather alert if one has been issued for the searched. "
  :alerts :alerts)

(defapi hourly
  " Returns an hourly forecast for the next 36 hours immediately following
        the api request. "
  :hourly :hourly_forecast)

(defapi yesterday " Returns a summary of the observed weather history for yesterday. "
  :yesterday :history)

(defapi hourly-seven-day " Returns an hourly forecast for the next 7 days. "
  :hourly7day :hourly_forecast)

(defapi forecast-seven-day
  " Returns a summary of the weather for the next 7 days. This includes high and
        low temperatures, a string text forecast, and the conditions. "
   :forecast7day :forecast)

;; This one is implemented a bit differently because it requires a date.
(defn history
  " Returns a summary of the observed weather for the specified date, which
        Should be a string of the format YYYYMMDD. "
  {:arglists '([key location date & [opts callback]] [key location date & [callback]])}
  [key location date & [s1 s2]]
  (if (or (fn? s1) (instance? clojure.lang.MultiFn s1))
    (api-call key (str " history_ " date) location :history nil s1)
    (api-call key (str " history_ " date) location :history s1 s2)))
