/*
 * Copyright (C) 2014 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.pigeonhole

import grails.web.JSONBuilder
import grails.util.Holders

import java.text.ParseException

/**
 * Command class for the sighting (based on DarwinCore terms)
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@grails.validation.Validateable
class SightingCommand {
    String userId
    String guid
    String scientificName
    List<String> tags = [].withDefault { new String() } // taxonomic tags
    String identificationVerificationStatus // identification confidence
    Integer individualCount
    List<MediaDto> multimedia = [].withDefault { new MediaDto() }
    String eventDate // can be date or ISO date with time
    String eventDateNoTime // date only
    String eventDateTime // ISO date + time
    String eventTime // time only
    String timeZoneOffset = (((TimeZone.getDefault().getRawOffset() / 1000) / 60) / 60)
    Double decimalLatitude
    Double decimalLongitude
    String geodeticDatum = "WGS84"
    Integer coordinateUncertaintyInMeters
    String georeferenceProtocol
    String locality
    String locationRemark
    String occurrenceRemarks
    String submissionMethod = "website"

    static constraints = {

        eventDateNoTime(blank: false, validator: {
            try {
                Date.parse('dd-MM-yyyy', it)
                return true
            } catch (ParseException e) {
                return false
            }
        })
        eventTime(nullable: true,  matches: "\\d{2}:\\d{2}(?::\\d{2})")
        //eventTime blank: false
    }

    public String getEventDate() {
        String dt
        if (eventDateTime) {
            dt = eventDateTime
        } else if (eventDateNoTime && eventTime) {
            def isoDate = getIsoDate(eventDateNoTime)
            def time = getValidTime(eventTime)

            if (isoDate && time) {
                dt = "${isoDate}T${eventTime}${timeZoneOffset?:'Z'}"
            }
        }

        dt
    }
    /**
     * Parse and check input date (Australian format DD-MM-YYYY) to
     * iso format (YYYY-MM-DD). If invalid, will return null
     *
     * @param input
     * @return
     */
    private String getIsoDate(String input) throws IllegalArgumentException {
        String output
        def dateBits = input.split('-')

        if (dateBits.length == 3) {
            output = dateBits.reverse().join('-') // Aus date to iso date format
        } else {
            throw new IllegalArgumentException("The date entered, " + input + " is invalid.")
        }

        output
    }

    /**
     * Parse and check input time (String).
     * If invalid, will return null
     *
     * @param input
     * @return
     */
    private String getValidTime(String input) throws IllegalArgumentException  {
        String output
        def timeBits = input.split(':')

        if (timeBits.length == 2) {
            // assume time without seconds
            timeBits.push("00")
        }

        if (timeBits.length == 3) {
            output = timeBits.join(':')
        } else {
            throw new IllegalArgumentException("The time entered, " + input + " is invalid.")
        }

        output
    }

    /**
     * Custom JSON method that allows fields to be excluded.
     * Code from: http://stackoverflow.com/a/5937793/249327
     *
     * @param excludes
     * @return JSON (String)
     */
    public String asJSON(List excludes = Holders.config.sighting.fields.excludes) {
        def wantedProps = [:]
        log.debug "excludes = ${excludes}"
        //this.properties.each { propName, propValue ->
        SightingCommand.declaredFields.findAll { !it.synthetic && !excludes.contains(it.name) }.each {
            log.debug "it: ${it.name} = $it || m - ${it.modifiers} || t - ${it.type}"
            def propName = it.name
            def propValue = this.getProperty(propName)
            log.debug "val: ${propValue} || ${propValue.getClass().name}"
            if (propValue instanceof List) {
                log.debug "List found"
                propValue = propValue.findAll {it} // remove empty and null values (and 0 and false)
            }
            if (propValue) {
                wantedProps.put(propName, propValue?:'')
            }
        }
        def builder = new JSONBuilder().build {
            wantedProps
        }
        log.debug "builder: ${builder.toString(true)}"

        builder.toString()
    }
}