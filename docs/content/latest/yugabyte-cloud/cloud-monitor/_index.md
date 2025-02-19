---
title: Alerts and monitoring
headerTitle: Alerts and monitoring
linkTitle: Alerts and monitoring
description: Set alerts and monitor your Yugabyte Cloud clusters.
image: /images/section_icons/explore/monitoring.png
headcontent: Set alerts and monitor cluster performance and activity.
section: YUGABYTE CLOUD
menu:
  latest:
    identifier: cloud-monitor
    weight: 100
---

Use Yugabyte Cloud alerts and monitoring to monitor cluster performance and be notified of potential problems.

### Alerts

To be automatically notified of potential problems, enable [alerts](cloud-alerts/) for cluster, database, and billing criteria. You configure alerts from the **Alerts** page.

### Performance monitoring

Monitor database and cluster performance in real time. Yugabyte Cloud provides the following tools to monitor your clusters:

- [Performance metrics](overview/). The cluster **Overview** and **Performance Metrics** tabs show a variety of performance metrics charted over time. Use cluster performance metrics to ensure the cluster configuration matches its performance requirements, and [scale the cluster vertically or horizontally](../cloud-clusters/configure-clusters/) as your requirements change.
- [Live queries](cloud-queries-live/). The cluster **Live Queries** tab shows the queries that are currently "in-flight" on your cluster.
- [Slow queries](cloud-queries-slow/). The cluster **YSQL Slow  Queries** tab shows queries run on the cluster, sorted by running time. Evaluate the slowest running YSQL queries that have been run on the cluster.

Access performance monitoring from the cluster **Performance** tab.

### Cluster properties

View cluster activity, node status, and database properties:

- Database tables. Use the cluster **Tables** tab to see the tables, databases, and namespaces on the cluster.
- Node status. Use the cluster **Nodes** tab to see the nodes in the cluster and their status.
- [Activity log](monitor-activity/). The cluster **Activity** tab provides a running audit of changes made to the cluster.

You access your clusters from the **Clusters** page.

<div class="row">

  <div class="col-12 col-md-6 col-lg-12 col-xl-6">
    <a class="section-link icon-offset" href="cloud-alerts/">
      <div class="head">
        <img class="icon" src="/images/section_icons/explore/monitoring.png" aria-hidden="true" />
        <div class="title">Alerts</div>
      </div>
      <div class="body">
        Enable alerts for cluster performance metrics and cloud billing.
      </div>
    </a>
  </div>

  <div class="col-12 col-md-6 col-lg-12 col-xl-6">
    <a class="section-link icon-offset" href="overview/">
      <div class="head">
        <img class="icon" src="/images/section_icons/explore/monitoring.png" aria-hidden="true" />
        <div class="title">View performance metrics</div>
      </div>
      <div class="body">
        Evaluate cluster performance with time series charts of performance metrics.
      </div>
    </a>
  </div>

  <div class="col-12 col-md-6 col-lg-12 col-xl-6">
    <a class="section-link icon-offset" href="cloud-queries-live/">
      <div class="head">
        <img class="icon" src="/images/section_icons/explore/monitoring.png" aria-hidden="true" />
        <div class="title">View live queries</div>
      </div>
      <div class="body">
        Monitor and display current running queries on your cluster.
      </div>
    </a>
  </div>

  <div class="col-12 col-md-6 col-lg-12 col-xl-6">
    <a class="section-link icon-offset" href="cloud-queries-slow/">
      <div class="head">
        <img class="icon" src="/images/section_icons/explore/monitoring.png" aria-hidden="true" />
        <div class="title">View slow YSQL queries</div>
      </div>
      <div class="body">
        Monitor and display past YSQL queries on your cluster.
      </div>
    </a>
  </div>

  <div class="col-12 col-md-6 col-lg-12 col-xl-6">
    <a class="section-link icon-offset" href="monitor-activity/">
      <div class="head">
        <img class="icon" src="/images/section_icons/explore/monitoring.png" aria-hidden="true" />
        <div class="title">Monitor cluster activity</div>
      </div>
      <div class="body">
        Review the activity on your cluster.
      </div>
    </a>
  </div>

</div>
