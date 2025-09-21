(ns galt.core.adapters.presentation-helpers
  (:require
    [markdown.core]
    [hiccup2.core]))

(defn render-markdown
  [s]
  (hiccup2.core/raw (markdown.core/md-to-html-string s)))
