(ns conduit.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [conduit.subs :as subs]))

;; -- Layout ------------------------------------------------------------------
;;
(defn header
  []
  (let [user @(subscribe [:user])] ;; create subscription for user
    [:nav.navbar.navbar-light
     [:div.container
      [:a.navbar-brand {:href "/#/"} "conduit"]
      (if (empty? user)
        [:ul.nav.navbar-nav.pull-xs-right
         [:li.nav-item
          [:a.nav-link.active {:href "/#/"} "Home"]]
         [:li.nav-item
          [:a.nav-link {:href "/#/login"} "Sign in"]]
         [:li.nav-item
          [:a.nav-link {:href "/#/register"} "Sign up"]]]
        [:ul.nav.navbar-nav.pull-xs-right
         [:li.nav-item
          [:a.nav-link.active {:href "/#/"} "Home"]]
         [:li.nav-item
          [:a.nav-link {:href "/#/editor"}
           [:i.ion-compose "New Article"]]]
         [:li.nav-item
          [:a.nav-link {:href "/#/settings"}
           [:i.ion-gear-a "Settings"]]]
         [:li.nav-item
          [:a.nav-link {:href (str "/#/@" (:username user))} (:username user)]]])]]))

(defn footer
  []
  [:footer
   [:div.container
    [:a.logo-font {:href "/#/"} "conduit"]
    [:span.attribution
     "An interactive learning project from "
     [:a {:href "https://thinkster.io"} "Thinkster"]
     ". Code & design licensed under MIT."]]])

;; -- Helpers -----------------------------------------------------------------
;;
(defn format-date
  [date]
  (.toDateString (js/Date. date)))

(defn tags-list
  [tags-list]
  [:ul.tag-list
   (for [tag tags-list]
     ^{:key tag} [:li.tag-default.tag-pill.tag-outline tag])])

(defn article-meta
  [{author          :author
    created-at      :createdAt
    likes           :likes
    favorites-count :favoritesCount
    favorited       :favorited
    slug            :slug}]
  (let [loading @(subscribe [:loading])
        user @(subscribe [:user])]
    [:div.article-meta
     [:a {:href (str "/#/@" (:username author))}
      [:img {:src (:image author)}]]
     " "
     [:div.info
      [:a.author {:href (str "/#/@" (:username author))} (:username author)]
      [:span.date (format-date created-at)]]
     (if (= (:username user) (:username author))
       [:span
        [:button.btn.btn-sm.btn-outline-secondary {:on-click #(dispatch [:delete-article slug])}
         [:i.ion-edit]
         [:span " Edit Article "]]
        " "
        [:button.btn.btn-outline-danger.btn-sm {:on-click #(dispatch [:delete-article slug])}
         [:i.ion-trash-a]
         [:span " Delete Article "]]]
       [:span
        [:button.btn.btn-sm.btn-outline-secondary {}
         [:i.ion-plus-round]
         [:span (str " Follow " (:username author))]]
        " "
        [:button.btn.btn-sm.btn-primary {:on-click #(dispatch [:toggle-favorite-article slug])
                                         :class (cond
                                                  (not favorited) "btn-outline-primary"
                                                  (:toggle-favorite-article loading) "disabled")}
         [:i.ion-heart]
         [:span (if favorited " Unfavorite Post " " Favorite Post ")]
         [:span.counter "(" favorites-count ")"]]])]))

(defn articles-preview
  [{:keys [description slug createdAt title author favoritesCount favorited tagList]}]
  (let [loading @(subscribe [:loading])]
    [:div.article-preview
     [:div.article-meta
      [:a {:href (str "/#/@" (:username author))}
       [:img {:src (:image author)}]]
      [:div.info
       [:a.author {:href (str "/#/@" (:username author))} (:username author)]
       [:span.date (format-date createdAt)]]
      [:button.btn.btn-primary.btn-sm.pull-xs-right {:on-click #(dispatch [:toggle-favorite-article slug])
                                                     :class (cond
                                                              (not favorited) "btn-outline-primary"
                                                              (:toggle-favorite-article loading) "disabled")}
       [:i.ion-heart " "]
       [:span favoritesCount]]]
     [:a.preview-link {:href (str "/#/article/" slug)}
      [:h1 title]
      [:p description]
      [:span "Read more ..."]
      [tags-list tagList]]])) ;; defined in Helpers section

(defn errors-list
  [errors]
  [:ul.error-messages
   (for [[key [val]] errors]
     ^{:key key} [:li (str (name key) " " val)])])

;; -- Home --------------------------------------------------------------------
;;
(defn get-articles [event params]
  (.preventDefault event)
  (dispatch [:get-articles params]))

(defn get-feed-articles [event params]
  (.preventDefault event)
  (dispatch [:get-feed-articles params]))

(defn home
  []
  (let [filter @(subscribe [:filter])
        tags @(subscribe [:tags])
        loading @(subscribe [:loading])
        articles @(subscribe [:articles])
        articles-count @(subscribe [:articles-count])
        user @(subscribe [:user])]
    [:div.home-page
     (when (empty? user)
       [:div.banner
        [:div.container
         [:h1.logo-font "conduit"]
         [:p "A place to share your knowledge."]]])
     [:div.container.page
      [:div.row
       [:div.col-md-9
        [:div.feed-toggle
         [:ul.nav.nav-pills.outline-active
          (when-not (empty? user)
            [:li.nav-item
             [:a.nav-link {:href ""
                           :class (when (:feed filter) "active")
                           :on-click #(get-feed-articles % {:tag nil :author nil :offset 0 :limit 10})} "Your Feed"]])
          [:li.nav-item
           [:a.nav-link {:href ""
                         :class (when-not (or (:tag filter) (:feed filter)) "active")
                         :on-click #(get-articles % {:tag nil :offset 0 :limit 10})} "Global Feed"]] ;; first argument: % is browser event, second: nil to remove filter by tags
          (when (:tag filter)
            [:li.nav-item
             [:a.nav-link.active
              [:i.ion-pound] (str " " (:tag filter))]])]]
        (if (:articles loading)
          [:div.article-preview
           [:p "Loading articles ..."]]
          (if (empty? articles)
            [:div.article-preview
             [:p "No articles are here... yet."]]
            (for [article articles]
              ^{:key (:slug article)} [articles-preview article])))
        (when-not (or (:articles loading) (< articles-count 10))
          [:ul.pagination
           (for [offset (range (/ articles-count 10))]
             ^{:key offset} [:li.page-item {:class (when (= (* offset 10) (:offset filter)) "active")
                                            :on-click #(get-articles % {:offset (* offset 10) :tag (:tag filter) :limit 10})}
                             [:a.page-link {:href ""} (+ 1 offset)]])])]

       [:div.col-md-3
        [:div.sidebar
         [:p "Popular Tags"]
         (if (:tags loading)
           [:p "Loading tags ..."]
           [:div.tag-list
            (for [tag tags]
              ^{:key tag} [:a.tag-pill.tag-default {:href "" :on-click #(get-articles % {:tag tag :limit 10 :offset 0})} tag])])]]]]]))

;; -- Login -------------------------------------------------------------------
;;
(defn login-user [event credentials]
  (.preventDefault event)
  (dispatch [:login credentials]))

(defn login
  []
  (let [default {:email "" :password ""}
        credentials (reagent/atom default)
        loading (subscribe [:loading])
        errors (subscribe [:errors])]
    (fn []
      (let [email (get @credentials :email)
            password (get @credentials :password)
            login-errors (get @errors :login)
            login-loading (get @loading :login)]
        [:div.auth-page
         (.log js/console login-errors loading)
         [:div.container.page
          [:div.row
           [:div.col-md-6.offset-md-3.col-xs-12
            [:h1.text-xs-center "Sign in"]
            [:p.text-xs-center
             [:a {:href "/#/register"} "Need an account?"]]
            (when login-errors
              [errors-list login-errors])
            [:form {:on-submit #(login-user % @credentials)}
             [:fieldset.form-group
              [:input.form-control.form-control-lg {:type "text"
                                                    :placeholder "Email"
                                                    :value email
                                                    :on-change #(swap! credentials assoc :email (-> % .-target .-value))
                                                    :disabled (when login-loading)}]]

             [:fieldset.form-group
              [:input.form-control.form-control-lg {:type "password"
                                                    :placeholder "Password"
                                                    :value password
                                                    :on-change #(swap! credentials assoc :password (-> % .-target .-value))
                                                    :disabled (when login-loading)}]]
             [:button.btn.btn-lg.btn-primary.pull-xs-right {:class (when login-loading "disabled")} "Sign in"]]]]]]))))

;; -- Register ----------------------------------------------------------------
;;
(defn register-user [event registration]
  (.preventDefault event)
  (dispatch [:register-user registration]))

(defn register
  []
  (let [default {:username "" :email "" :password ""}
        registration (reagent/atom default)
        loading (subscribe [:loading])
        errors (subscribe [:errors])]
    (fn []
      (let [username (get @registration :username)
            email (get @registration :email)
            password (get @registration :password)
            register-user-errors (get @errors :register-user)
            register-user-loading (get @loading :register-user)]
        [:div.auth-page
         [:div.container.page
          [:div.row
           [:div.col-md-6.offset-md-3.col-xs-12
            [:h1.text-xs-center "Sign up"]
            [:p.text-xs-center
             [:a {:href "/#/login"} "Have an account?"]]
            (when register-user-errors
              [errors-list register-user-errors])
            [:form {:on-submit #(register-user % @registration)}
             [:fieldset.form-group
              [:input.form-control.form-control-lg {:type "text"
                                                    :placeholder "Your Name"
                                                    :value username
                                                    :on-change #(swap! registration assoc :username (-> % .-target .-value))
                                                    :disabled (when register-user-loading)}]]
             [:fieldset.form-group
              [:input.form-control.form-control-lg {:type "text"
                                                    :placeholder "Email"
                                                    :value email
                                                    :on-change #(swap! registration assoc :email (-> % .-target .-value))
                                                    :disabled (when register-user-loading)}]]
             [:fieldset.form-group
              [:input.form-control.form-control-lg {:type "password"
                                                    :placeholder "Password"
                                                    :value password
                                                    :on-change #(swap! registration assoc :password (-> % .-target .-value))
                                                    :disabled (when register-user-loading)}]]
             [:button.btn.btn-lg.btn-primary.pull-xs-right {:class (when register-user-loading "disabled")} "Sign up"]]]]]]))))

(defn profile
  []
  (let [profile @(subscribe [:profile])
        filter @(subscribe [:filter])
        loading @(subscribe [:loading])
        articles @(subscribe [:articles])
        user @(subscribe [:user])]
    [:div.profile-page
     [:div.user-info
      [:div.container
       [:div.row
        [:div.col-xs-12.col-md-10.offset-md-1
         [:img.user-img {:src (:image profile)}]
         [:h4 (:username profile)]
         [:p (:bio profile)]
         (if (= (:username user) (:username profile))
           [:a.btn.btn-sm.btn-outline-secondary.action-btn {:href "/#/settings"}
            [:i.ion-gear-a] " Edit Profile Settings"]
           [:button.btn.btn-sm.action-btn.btn-outline-secondary {:on-click #(dispatch [:toggle-follow-user (:username profile)])
                                                                 :class (when (:toggle-follow-user loading) "disabled")}
            [:i {:class (if (:following profile) "ion-minus-round" "ion-plus-round")}]
            [:span (if (:following profile) (str " Unfollow " (:username profile)) (str " Follow " (:username profile)))]])]]]]
     [:div.container
      [:row
       [:div.col-xs-12.col-md-10.offset-md-1
        [:div.articles-toggle
         [:ul.nav.nav-pills.outline-active
          [:li.nav-item
           [:a.nav-link {:href (str "/#/@" (:username profile)) :class (when (:author filter) " active")} "My Articles"]]
          [:li.nav-item
           [:a.nav-link {:href (str "/#/@" (:username profile) "/favorites") :class (when (:favorites filter) "nav-link active")} "Favorited Articles"]]]]
        (if (empty? articles)
          [:div.article-preview
           [:p "No articles are here... yet."]]
          (for [article articles]
            ^{:key (:slug article)} [articles-preview article]))
        (when (:articles loading)
          [:div.article-preview
           [:p "Loading articles ..."]])]]]]))

;; -- Settings ----------------------------------------------------------------
;;
(defn logout-user [event]
  (.preventDefault event)
  (dispatch [:logout]))

(defn update-user [event update]
  (.preventDefault event)
  (js/console.log update))
; (dispatch [:update-user user]))

(defn settings
  []
  (let [user @(subscribe [:user])
        loading @(subscribe [:loading])
        default {:image "" :username "" :bio "" :email "" :password ""}
        user-update (reagent/atom default)]
    [:div.settings-page
     [:div.container.page
      [:div.row
       [:div.col-md-6.offset-md-3.col-xs-12
        [:h1.text-xs-center "Your Settings"]
        [:form
         [:fieldset
          [:fieldset.form-group
           [:input.form-control {:type "text"
                                 :placeholder "URL of profile picture"
                                 :default-value (:image user)
                                 :on-change #(swap! user-update assoc :image (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:input.form-control.form-control-lg {:type "text"
                                                 :placeholder "Your Name"
                                                 :default-value (:username user)
                                                 :on-change #(swap! user-update assoc :username (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:textarea.form-control.form-control-lg {:rows "8"
                                                    :placeholder "Short bio about you"
                                                    :default-value (:bio user)
                                                    :on-change #(swap! user-update assoc :bio (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:input.form-control.form-control-lg {:type "text"
                                                 :placeholder "Email"
                                                 :default-value (:email user)
                                                 :on-change #(swap! user-update assoc :email (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:input.form-control.form-control-lg {:type "password"
                                                 :placeholder "Password"
                                                 :default-value ""
                                                 :on-change #(swap! user-update assoc :password (-> % .-target .-value))}]]
          [:button.btn.btn-lg.btn-primary.pull-xs-right {:on-click #(update-user % @user-update)
                                                         :class (when (:update-user loading) "disabled")} "Update Settings"]]]
        [:hr]
        [:button.btn.btn-outline-danger {:on-click #(logout-user %)} "Or click here to logout."]]]]]))

;; -- Editor ------------------------------------------------------------------
;;
(defn upsert-article [event content slug]
  (.preventDefault event)
  (dispatch [:upsert-article {:article content :slug slug}]))

(defn editor
  []
  (let [{:keys [title description body tagList slug]} @(subscribe [:article])
        default {:title title :description description :body body :tagList tagList}
        content (reagent/atom default)]
    [:div.editor-page
     [:div.container.page
      [:div.row
       [:div.col-md-10.offset-md-1.col-xs-12
        [:form
         [:fieldset
          [:fieldset.form-group
           [:input.form-control.form-control-lg {:type "text"
                                                 :placeholder "Article Title"
                                                 :default-value title
                                                 :on-change #(swap! content assoc :title (-> % .-target .-value))}]]

          [:fieldset.form-group
           [:input.form-control {:type "text"
                                 :placeholder "What's this article about?"
                                 :default-value description
                                 :on-change #(swap! content assoc :description (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:textarea.form-control {:rows "8"
                                    :placeholder "Write your article (in markdown)"
                                    :default-value body
                                    :on-change #(swap! content assoc :body (-> % .-target .-value))}]]
          [:fieldset.form-group
           [:input.form-control {:type "text"
                                 :placeholder "Enter tags"
                                 :default-value tagList
                                 :on-change #(swap! content assoc :tagList (-> % .-target .-value))}]
           [:div.tag-list]]
          [:button.btn.btn-lg.pull-xs-right.btn-primary {:type "button" :on-click #(upsert-article % @content slug)} "Publish Article"]]]]]]]))

;; -- Article -----------------------------------------------------------------
;;
(defn post-comment [event body]
  (.preventDefault event)
  (dispatch [:post-comment body]))

(defn article
  []
  (let [article @(subscribe [:article])
        user @(subscribe [:user])
        profile @(subscribe [:profile])
        comments @(subscribe [:comments])
        default {:body ""}
        body (reagent/atom default)]
    [:div.article-page
     [:div.banner
      [:div.container
       [:h1 (:title article)]
       [article-meta article]]] ;; defined in Helpers section
     [:div.container.page
      [:div.row.article-content
       [:div.col-md-12
        [:p (:body article)]]]
      [tags-list (:tagList article)] ;; defined in Helpers section
      [:hr]
      [:div.article-actions
       [article-meta article]] ;; defined in Helpers section
      [:div.row
       [:div.col-xs-12.col-md-8.offset-md-2
        (if user
          [:form.card.comment-form
           [:div.card-block
            [:textarea.form-control {:placeholder "Write a comment..."
                                     :rows "3"
                                     :on-change #(swap! body assoc :body (-> % .-target .-value))}]]
           [:div.card-footer
            [:img.comment-author-img {:src (:image user)}]
            [:button.btn.btn-sm.btn-primary {:on-click #(post-comment % @body)} "Post Comment"]]]
          [:p
           [:a {:href "#/register"} "Sign in"]
           " or "
           [:a {:href "#/login"} "Sign in"]
           " to add comments on this article."])
        (if (empty? comments)
          [:div]
          (for [{:keys [id createdAt body author]} comments]
            ^{:key id} [:div.card
                        [:div.card-block
                         [:p.card-text body]]
                        [:div.card-footer
                         [:a.comment-author {:href (str "/#/@" (:username author))}
                          [:img.comment-author-img {:src (:image author)}]]
                         " "
                         [:a.comment-author {:href (str "/#/@" (:username author))} (:username author)]
                         [:span.date-posted (format-date createdAt)]
                         (when (= (:username user) (:username profile))
                           [:span.mod-options {:on-click #(dispatch [:delete-comment id])}
                            [:i.ion-trash-a]])]]))]]]]))

(defn- pages [page-name]
  (case page-name
    :home [home]
    :login [login]
    :register [register]
    :profile [profile]
    :settings [settings]
    :editor [editor]
    :article [article]
    [home]))

(defn conduit-app
  []
  (let [active-page @(subscribe [:active-page])]
    [:div
     [header]
     [pages active-page]
     [footer]]))
