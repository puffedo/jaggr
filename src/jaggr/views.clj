(ns jaggr.views
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [formative.core :as f]
            [formative.parse :as fp]
            [hiccup.core :refer :all]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer :all]
            [jaggr.jenkins :refer :all]
            [omniconf.core :as config]
            [trptcolin.versioneer.core :as version]))


;;
;; common page elements
;;

(defn header [& {:keys [meta-tag]}]
  [:head
   [:title "JAGGR"]
   (include-css "/css/style.css")
   (include-css "https://fonts.googleapis.com/css?family=Open+Sans:400,700,400italic")
   [:link {:rel "shortcut icon" :href "img/favicon.ico"}]
   (when meta-tag meta-tag)])


(defn reload-header []
  (header :meta-tag [:meta {:http-equiv "refresh" :content (str (config/get :refresh-rate))}]))


(defn fullscreen-body [& content]
  [:body
   (link-to "/config"
            [:img#logo {:src   "/img/jaggr-logo.png"
                        :title (str "click to configure\n"
                                    "version " (version/get-version "jaggr" "jaggr" "N/A"))}])
   [:div.fullscreen content]])


;;
;; config page
;;

(def config-form
  {:fields      [{:name :base-url :label "Jenkins Base URL" :blank-nil true}
                 {:name :user :label "User name" :blank-nil true}
                 {:name :user-token :label "User-Token" :type :password :blank-nil true}
                 {:name :refresh-rate :label "Refresh rate (in seconds)" :datatype :int}
                 {:name :image-url :label "Default URL for background images" :blank-nil true}
                 {:name :image-url-red :label "URL for red page background image" :blank-nil true}
                 {:name :image-url-yellow :label "URL for yellow page background image" :blank-nil true}
                 {:name :image-url-green :label "URL for green page background image" :blank-nil true}
                 {:name :image-url-error :label "URL for error page background image" :blank-nil true}]
   :validations [[:required [:base-url]]
                 [:url [:base-url :image-url :image-url-red :image-url-yellow
                        :image-url-green :image-url-error]]
                 [:matches #".*/$" [:base-url] "must end with a slash '/'"]
                 [:min-val 10 [:refresh-rate]]]})


(defn config-page [& {:keys [problems]}]
  (html5
    (header)
    (fullscreen-body
      [:img {:src "/background-image-error"}]
      [:div.fullscreen.error
       [:h1 "WHERE IS MY JENKINS?"]
       [:div.subtext "please provide some configuration parameters"]
       [:div (f/render-form (assoc config-form
                              :values (config/get)
                              :problems problems))]])))

(defn submit-config-form [params]
  (fp/with-fallback
    #(config-page :problems %)
    (let [p (fp/parse-params config-form params)]
      (doseq [[k v] p] (config/set k v))
      (try
        (config/verify :quit-on-error false)
        {:status  302
         :headers {"Location" "/"}
         :body    ""}
        (catch Exception _
          {:status  302
           :headers {"Location" "/config"}
           :body    ""})))))


;;
;; index page
;;

(defn job-details [job]
  (when job
    [:div.job
     (link-to {:class "job-name"} (:lastCompletedBuildUrl job) (h (:name job)))
     (when (:claimedBy job)
       [:div.job-claimed-by "was heroically claimed by " (h (:claimedBy job))])
     (when (:reason job)
       [:div.job-reason "'" (h (:reason job)) "'"])]))


(defn job-list [jobs]
  (when jobs
    [:div.job-list
     (for [job jobs] (job-details job))]))


(defn index-page []
  (try
    (let [failed-jobs (get-failed-jobs)]
      (html5
        (reload-header)
        (fullscreen-body
          (cond
            (not-empty (:unclaimed failed-jobs))
            [:div.fullscreen
             [:img {:src "/background-image-red"}]
             [:div.fullscreen.red
              [:h1 "BROKEN BUILDS - CLAIM AND FIX THEM!"]
              [:div.subtext
               "have a look at the broken builds - find someone who can fix the problem - claim the builds and fix them"]
              (job-list (:unclaimed failed-jobs))]]

            (not-empty (:claimed failed-jobs))
            [:div.fullscreen
             [:img {:src "/background-image-yellow"}]
             [:div.fullscreen.yellow
              [:h1 "BROKEN BUILDS - HELP IS ON THE WAY!"]
              [:div.subtext
               "see if you can help - check in with care - be careful with merges"]
              (job-list (:claimed failed-jobs))]]

            (not-empty (:unclaimable failed-jobs))
            [:div.fullscreen
             [:img {:src "/background-image-green"}]
             [:div.fullscreen.green
              [:h1 "MOST BUILDS ARE MOSTLY OK"]
              [:div.subtext "maybe have a look at some of these jobs as well"]
              (job-list (:unclaimable failed-jobs))]]

            :else
            [:div.fullscreen
             [:img {:src "/background-image-green"}]
             [:div.fullscreen.green
              [:h1 "HOORAY!"]
              [:div.subtext "all builds are green! - better go home now..."]]]))))

    (catch Exception e
      (log/error e "Something is wrong here!")
      (html5
        (reload-header)
        (fullscreen-body
          [:div.fullscreen
           [:img {:src "/background-image-error"}]
           [:div.fullscreen.error
            [:h1 "SOMETHING IS WRONG HERE"]
            [:div.subtext "is Jenkins accessible? - wrong parameters? - network problems?"]
            [:div "I tried to access " (config/get :base-url)]
            [:div "trying again every " (config/get :refresh-rate) " seconds"]
            [:br]
            (link-to {:class "config-link"} "/config" "Change configuration parameters")]])))))


;;
;; background image handling
;;

;; returns a random image file from the provided directory or nil if the directory is empty,
;; contains no images or doesn't exist.
(defn- random-image-from [dir]
  (let
    [img-files (->> dir
                    (io/file)
                    (file-seq)
                    (filter #(not (.isDirectory %1)))
                    (filter #(re-find #"([^\s]+(\.(?i)(jpg|jpeg|png|gif|bmp))$)" (.getPath %1))))]
    (when (not-empty img-files) (rand-nth img-files))))

;; returns an http response that contains an image from the provided directory or
;; redirects to an image service if not such file exists. The image service url
;; can be provided or left empty, so a fallback service is used
(defn- image-from [dir url]
  (if-let [image (random-image-from dir)]
    ;; file exists
    {:status 200
     :body   image}
    ;; no file is found
    {:status  302
     :headers {"Location" (or url (config/get :image-url))}
     :body    ""}))

(defn background-image-red []
  "returns an http response that contains a background image for red screens"
  (image-from "images/red/" (config/get :image-url-red)))

(defn background-image-yellow []
  "returns an http response that contains a background image for yellow screens"
  (image-from "images/yellow/" (config/get :image-url-yellow)))

(defn background-image-green []
  "returns an http response that contains a background image for green screens"
  (image-from "images/green/" (config/get :image-url-green)))

(defn background-image-error []
  "returns an http response that contains a background image for error screens"
  (image-from "images/error/" (config/get :image-url-error)))

