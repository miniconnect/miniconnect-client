<#list releases as release>
miniconnect-client (${release.version}) unstable; urgency=medium

<#list release.sections as section>
  * ${section.title}
<#list section.entries as entry>
    - ${entry}
</#list>
</#list>

 -- Dávid Horváth <horvath@webarticum.hu>  ${release.date}

</#list>
